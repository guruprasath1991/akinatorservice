const { Aki } = require('aki-api');
const readline = require('readline');
var express = require('express');
const path = require('path');
const serverless = require('serverless-http');
var app = express();
const router = express.Router();
const http = require('http');

/**
 * Get port from environment and store inExpress.
 */
const port = normalizePort(process.env.PORT || '3000');
app.set('port', port);

/**
 * Create HTTP server.
 */
const server = http.createServer(app);

/**
 * Listen on provided port, on all network interfaces.
 */

server.listen(port);
console.log("listening to port : "+port);
/**
 * Normalize a port into a number, string, or false.
 */
function normalizePort(val) {
	const port = parseInt(val, 10);

	if (isNaN(port)) {
		// named pipe
		return val;
	}

	if (port >= 0) {
		// port number
		return port;
	}

	return false;
}

//app.use('/', router);
app.use('/.netlify/functions/server', router);  // path must route to lambda
app.use('/', (req, res) => res.sendFile(path.join(__dirname, 'index.html')));

//Global Constants
const errorPrefix = "##Error##~";
let langMap = {
	"english" : "en",
	"arabic" : "ar",
	"chinese" : "cn",
	"german" : "de",
	"spanish" : "es",
	"french" : "fr",
	"hebrew" : "il",
	"italian" : "it",
	"japanese" : "jp",
	"korean" : "kr",
	"dutch" : "nl",
	"polish" : "pl",
	"portuguese" : "pt",
	"russian" : "ru",
	"turkish" : "tr"
}
let apiPool = {};
let declinedGuesses = {};
let guessesPool = {};

router.get('/start', function(req, res) {
	try {
		let userCode = req.query.userCode;
		let userLang = req.query.userLang;

		if(userCode && userLang) {
			let region = langMap[userLang] ? langMap[userLang] : "en";
			start(res, region, userCode);
		}
		else {
			res.send(errorPrefix + "Invalid request");
		}
	}
	catch (e) {
		res.send(errorPrefix + "Could not Contact Server. Check your Internet connection and Try Again");
	}
});

router.get('/yes', function(req, res) {
	receiveUserAnswer(req, res, 0);
});

router.get('/no', function(req, res) {
	receiveUserAnswer(req, res, 1);
});

router.get('/dontknow', function(req, res) {
	receiveUserAnswer(req, res, 2);
});

router.get('/probably', function(req, res) {
	receiveUserAnswer(req, res, 3);
});

router.get('/probablynot', function(req, res) {
	receiveUserAnswer(req, res, 4);
});

router.get('/undo', function(req, res) {
	handleUndo(req, res);
});

router.get('/continue', function(req, res) {
    receiveUserAnswer(req, res, null, true);
});

router.get('/cleanServer555', function(req, res) {
    try {
        apiPool = {};
        declinedGuesses = {};
        guessesPool = {};
        res.send("Server Cleaned");
    }
    catch (e) {
        res.send("Error while cleaning Server");
    }
});

router.get('/getServerConsolidatedPool', function(req, res) {
    getConsolidatedPool(res);
});

const receiveUserAnswer = function (req, res, userAnswer, isContinue) {
	try {
		let userCode = req.query.userCode;

		if(userCode) {
			let aki = apiPool[userCode];
			if(aki)
			{
			    if(isContinue)
                {
                    sendQuestion(res, aki);
                }
			    else {
                    whatsNext(res, aki, userCode, userAnswer);
                }
			}
			else {
                handleError(res, userCode);
			}
		}
		else {
            handleError(res);
		}
	}
	catch (e) {
        handleError(res);
	}
}

const handleUndo = async function (req, res) {
	try {
		let userCode = req.query.userCode;

		if(userCode) {
			let aki = apiPool[userCode];
			if(aki)
			{
				await aki.back();
				sendQuestion(res, aki);
			}
			else {
                handleError(res, userCode);
			}
		}
		else {
            handleError(res);
		}
	}
	catch (e) {
        handleError(res);
	}
}

const start = async function(res, region, userCode) {
	try {
		let aki = new Aki(region);
		initializeFreshForUser(aki, userCode);
		/*if(region == "en") {
            try {
                await aki.start();
            } catch (e) {
                aki.urlApiWs = "srv11.akinator.com:9346";
                aki.gameEnv.urlApiWs = "srv11.akinator.com:9346";
                await aki.start();
            }
        }
		else
        {
            await aki.start();
        }*/
        try {
            await aki.start();
        } catch (e) {
            try {
                aki.urlApiWs = "srv11.akinator.com:9346";
                aki.gameEnv.urlApiWs = "srv11.akinator.com:9346";
                await aki.start();
            }
            catch (e) {
                aki.urlApiWs = "srv3.akinator.com:9333";
                aki.gameEnv.urlApiWs = "srv3.akinator.com:9333";
                await aki.start();
            }
        }
		res.send("1. "+aki.question);
	}
	catch (e) {
		res.send(errorPrefix + "Server down or Technical Error. Please, Try again.");
	}
}

const whatsNext = async function(res, aki, userCode, userAnswer) {
	try {
		await aki.step(userAnswer);
        if(aki.currentStep >= 78 || !aki.question) //provide guesses - if not won yet - stimulate win and collect guesses
        {
            sendLoseResult(res, aki, userCode);
        }
        else {
            if (aki.progress >= 80) {
                if(isUserAnswersObjectified(aki))
                {
                    /*if(!declinedGuesses[userCode].includes(aki.answers[0].id))
                    {
                        sendWinResult(res, aki, userCode);
                    }
                    else
                    {
                        sendQuestion(res, aki);
                    }*/
                    //Since we can honour results other than 1st result if their probability is >=80
                    sendWinResult(res, aki, userCode);
                }
                else {
                    await aki.win();
                    if(isUserAnswersObjectified(aki)) {
                        sendWinResult(res, aki, userCode);
                    }
                    else
                    {
                        sendQuestion(res, aki);
                    }
                }
            } else {
                sendQuestion(res, aki);
            }
        }
        //test1(res, aki, userCode, userAnswer);
	}
	catch (e) {
		//res.send(errorPrefix + "Technical Error. Please, Try again.");
        handleError(res, userCode);
	}
}


//Util methods

const sendWinResult = function(res, aki, userCode) {
    let winStr = "";

    if(!declinedGuesses[userCode].includes(aki.answers[0].id)) {
        declinedGuesses[userCode].push(aki.answers[0].id);
        winStr += getAnswerString(aki.answers[0]);
    }

    try {
        for (let index = 1; index < aki.answers.length; index++) {
            let currProbabilityStr = aki.answers[index].proba;
            let currProbabilityNum = parseFloat(currProbabilityStr);

            if (currProbabilityNum >= .8) {
                if(!declinedGuesses[userCode].includes(aki.answers[index].id)) {
                    declinedGuesses[userCode].push(aki.answers[index].id);
                    winStr += getAnswerString(aki.answers[index]);
                }
            }
        }
    }
    catch (e) {
        //No handling needed
    }

    if(winStr && winStr!="") {
        saveGuesses(userCode, aki);
        res.send(winStr);
    }
    else {
        sendQuestion(res, aki);
    }
}

const sendQuestion = function(res, aki) {
    res.send((aki.currentStep + 1) + ". " + aki.question);
}

const sendLoseResult = function(res, aki, userCode) {
    saveGuesses(userCode, aki);
    let userGuesses = guessesPool[userCode];

    if(userGuesses && userGuesses.length>0)
    {
        deDupeGuesses(userCode);
        userGuesses.reverse();
        let guessesStrToRet = getGuessesString(userCode);
        if(userCode) {
            optimisePools(userCode);
        }
        res.send(guessesStrToRet);
    }
    else {
        if(userCode) {
            optimisePools(userCode);
        }
        res.send("akinatorlostwithoutguesses");
    }
}

const getAnswerString = function(answer) {
    let imagePath = answer.absolute_picture_path;
    if(imagePath && imagePath.endsWith("/none.jpg"))
    {
        imagePath = "-";
    }
    return "###" + answer.name + "~" + answer.description + "~" + imagePath + "~" + answer.proba;
}

const getGuessesString = function(userCode) {
    let userGuesses = guessesPool[userCode];
    let userGuessesStr = "";

    userGuesses.forEach(function(currGuess) {
        let imagePath = currGuess.absolute_picture_path;
        if(imagePath && imagePath.endsWith("/none.jpg"))
        {
            imagePath = "-";
        }
        userGuessesStr += "@@@" + currGuess.name + "~" + currGuess.description + "~" + imagePath + "~" + currGuess.proba;
    });

    return (userGuessesStr + "--akinatorlostwithguesses");
}

const saveGuesses = function(userCode, aki) {
    let userGuesses = guessesPool[userCode];
    if(isUserAnswersObjectified(aki)) {
        for (let currAnsIndex = 1; currAnsIndex<aki.answers.length; currAnsIndex++)
        {
            let currAns = aki.answers[currAnsIndex];
            if(!declinedGuesses[userCode].includes(currAns.id))
            {
                if(!isGuessesContainsGuess(userCode, currAns))
                {
                    userGuesses.push(currAns);
                }
            }
        }
    }
}

const deDupeGuesses = function(userCode) {
    let userGuesses = guessesPool[userCode];
    let dedupedGuesses = [];

    userGuesses.forEach(function(currGuess) {
        if(!declinedGuesses[userCode].includes(currGuess.id))
        {
            dedupedGuesses.push(currGuess);
        }
    });

    guessesPool[userCode] = dedupedGuesses;
}

const isUserAnswersObjectified = function(aki)
{
    let toRet = false;
    if(aki.answers && aki.answers[0] && aki.answers[0].name)
    {
        toRet = true;
    }
    return toRet;
}

const isGuessesContainsGuess = function(userCode, guessToChk)
{
    let toRet = false;
    let userGuesses = guessesPool[userCode];
    userGuesses.forEach(function(currGuess) {
        if(currGuess.id == guessToChk.id)
        {
            toRet = true;
        }
    });
    return toRet;
}

const initializeFreshForUser = function(aki, userCode) {
    try {
        apiPool[userCode] = aki;
        declinedGuesses[userCode] = [];
        guessesPool[userCode] = [];
    }
    catch (e) {
        //handling not needed as of now
    }
}

const optimisePools = function(userCode) {
    try {
        delete apiPool[userCode];
        delete declinedGuesses[userCode];
        delete guessesPool[userCode];
    }
    catch (e) {
        //handling not needed as of now
    }
}

const getConsolidatedPool = function(res) {
    try {
        let consolidatedPool = {};
        consolidatedPool["apiPool"] = apiPool;
        consolidatedPool["guessesPool"] = guessesPool;
        consolidatedPool["declinedGuesses"] = declinedGuesses;
        res.send(consolidatedPool);
    }
    catch (e) {
        res.send(e+" : Error in getting pool");
    }
}

const handleError = function(res, userCode) {
    if(userCode) {
        optimisePools(userCode);
    }
    res.send(errorPrefix + "Session Timed out, please Start Again");
}


//Test Methods

const test1 = async function(res, aki, userCode, userAnswer) {
    let answerStr = "";
    if (aki.progress >= 80 || aki.currentStep >= 78) {
        await aki.win();
    }
    if(aki.answers)
    {
        aki.answers.forEach(function(currAns) {
            if(currAns.name)
            {
                answerStr= answerStr+"~"+currAns.name
            }
            if(currAns.description)
            {
                answerStr= answerStr+"---"+currAns.description
            }
            if(currAns.proba)
            {
                answerStr= answerStr+"---"+currAns.proba
            }
        });
    }
    res.send((aki.currentStep+1) + ". " + aki.question + " ~ " + aki.progress + " ~ " + answerStr);
}

const processAnswersAndGuesses = function(userCode, answers) {
    try {
        let toRetStr = "";
        answers.forEach(function(currAns) {
            if(currAns.name)
            {
                answerStr= answerStr+"~"+currAns.name
            }
            if(currAns.description)
            {
                answerStr= answerStr+"---"+currAns.description
            }
            if(currAns.proba)
            {
                answerStr= answerStr+"---"+currAns.proba
            }
        });
    }
    catch (e) {
        //handling not needed as of now
    }
}

module.exports = app;
module.exports.handler = serverless(app);