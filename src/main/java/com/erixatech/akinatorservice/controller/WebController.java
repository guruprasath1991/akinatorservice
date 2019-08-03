package com.erixatech.akinatorservice.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.markozajc.akiwrapper.Akiwrapper;
import com.markozajc.akiwrapper.Akiwrapper.Answer;
import com.markozajc.akiwrapper.AkiwrapperBuilder;
import com.markozajc.akiwrapper.core.entities.Guess;
import com.markozajc.akiwrapper.core.entities.Question;
import com.markozajc.akiwrapper.core.entities.Server.Language;
import com.markozajc.akiwrapper.core.utils.Servers;

@RestController
public class WebController {

	HashMap<String, Akiwrapper> apiPool = new HashMap<>();
	String errorPrefix = "##Error##~";
	HashMap<String, List<Long>> declinedGuesses = new HashMap<>();
	HashMap<String, List<Guess>> guessesPool = new HashMap<>();

	@RequestMapping("/start")
	public String start(@RequestParam String userCode, @RequestParam String userLang) {
		String toRetStr = "";
		try {

			Akiwrapper aw = new AkiwrapperBuilder().setFilterProfanity(true)
					.setLocalization(getUserLanguage(userLang.toLowerCase().trim())).build();
			initializeFreshForUser(aw, userCode);
			Question q = aw.getCurrentQuestion();
			toRetStr = (q.getStep() + 1) + ". " + q.getQuestion();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			toRetStr = errorPrefix + "Could not Contact Server. Check your Internet connection and Try Again";
		}
		return toRetStr;
	}

	public void initializeFreshForUser(Akiwrapper aw, String userCode) {
		apiPool.put(userCode, aw);
		declinedGuesses.put(userCode, new ArrayList<>());
		guessesPool.put(userCode, new ArrayList<>());
	}

	public Language getUserLanguage(final String userLang) {
		Language toRetLang = Language.ENGLISH;
		List<Language> languages = new ArrayList<>(Servers.SERVER_GROUPS.keySet());
		toRetLang = languages.stream().filter(l -> l.toString().toLowerCase().equals(userLang)).findAny()
				.orElse(Language.ENGLISH);
		return toRetLang;
	}

	@RequestMapping("/cleanServer555")
	public String cleanServer() {
		String toRet = "Server Cleaned";
		try {
			apiPool.clear();
			declinedGuesses.clear();
			guessesPool.clear();
		} catch (Exception ex) {
			toRet = "Error while cleaning Server";
		}
		return toRet;
	}

	/*
	 * @RequestMapping("/languages") public String languages(@RequestParam String
	 * userCode) { String toRetStr = ""; try { List<Language> languages = new
	 * ArrayList<>(Servers.SERVER_GROUPS.keySet()); for (Language lang : languages)
	 * { try { Akiwrapper aw = new
	 * AkiwrapperBuilder().setFilterProfanity(true).setLocalization(lang).build();
	 * if (aw != null) { toRetStr = toRetStr + aw.getCurrentQuestion().getQuestion()
	 * + "~" + lang.name() + "#"; } } catch (Exception ex) {
	 * 
	 * }
	 * 
	 * } } catch (Exception e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); toRetStr = errorPrefix +
	 * "Server Error, please Start Again"; } return toRetStr; }
	 */

	@RequestMapping("/yes")
	public String answerYes(@RequestParam String userCode) {
		String toRetStr = "";
		try {
			Akiwrapper currentAw = apiPool.get(userCode);
			currentAw.answerCurrentQuestion(Answer.YES);
			toRetStr = whatsNext(currentAw, userCode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			toRetStr = errorHandle(e, userCode);
		}
		return toRetStr;
	}

	@RequestMapping("/no")
	public String answerNo(@RequestParam String userCode) {
		String toRetStr = "";
		try {
			Akiwrapper currentAw = apiPool.get(userCode);
			currentAw.answerCurrentQuestion(Answer.NO);
			toRetStr = whatsNext(currentAw, userCode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			toRetStr = errorHandle(e, userCode);
		}
		return toRetStr;
	}

	@RequestMapping("/undo")
	public String answerUndo(@RequestParam String userCode) {
		String toRetStr = "";
		try {
			Akiwrapper currentAw = apiPool.get(userCode);
			Question q = currentAw.undoAnswer();
			toRetStr = (q.getStep() + 1) + ". " + q.getQuestion();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			toRetStr = errorHandle(e, userCode);
		}
		return toRetStr;
	}

	@RequestMapping("/dontknow")
	public String answerDontKnow(@RequestParam String userCode) {
		String toRetStr = "";
		try {
			Akiwrapper currentAw = apiPool.get(userCode);
			currentAw.answerCurrentQuestion(Answer.DONT_KNOW);
			toRetStr = whatsNext(currentAw, userCode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			toRetStr = errorHandle(e, userCode);
		}
		return toRetStr;
	}

	@RequestMapping("/probably")
	public String answerProbably(@RequestParam String userCode) {
		String toRetStr = "";
		try {
			Akiwrapper currentAw = apiPool.get(userCode);
			currentAw.answerCurrentQuestion(Answer.PROBABLY);
			toRetStr = whatsNext(currentAw, userCode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			toRetStr = errorHandle(e, userCode);
		}
		return toRetStr;
	}

	@RequestMapping("/probablynot")
	public String answerProbablyNot(@RequestParam String userCode) {
		String toRetStr = "";
		try {
			Akiwrapper currentAw = apiPool.get(userCode);
			currentAw.answerCurrentQuestion(Answer.PROBABLY_NOT);
			toRetStr = whatsNext(currentAw, userCode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			toRetStr = errorHandle(e, userCode);
		}
		return toRetStr;
	}

	@RequestMapping("/continue")
	public String continueWithGuesses(@RequestParam String userCode) {
		String toRetStr = "";
		try {
			toRetStr = whatsNext(apiPool.get(userCode), userCode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			toRetStr = errorHandle(e, userCode);
		}
		return toRetStr;
	}

	public String whatsNext(Akiwrapper currentAw, String userCode) {
		boolean isGuessMatches = false;
		String toRetStr = "";
		try {
			for (Guess guess : currentAw
					.getGuessesAboveProbability(0.85 /* you can specify your threshold between 0 and 1 */)) {
				if (!declinedGuesses.get(userCode).contains(Long.valueOf(guess.getIdLong()))) {
					// Do something with those guesses
					isGuessMatches = true;
					toRetStr += "###" + guess.getName() + "~" + guess.getDescription() + "~" + guess.getImage() + "~"
							+ guess.getProbability();
					declinedGuesses.get(userCode).add(guess.getIdLong());
				}
			}

			saveGuesses(currentAw, userCode); // Save the Guesses

			if (!isGuessMatches) {
				Question q = currentAw.getCurrentQuestion();
				if (q == null) {
					/*
					 * // Watch out! Akinator has ran out of questions! // In this case, // -
					 * Akiwrapper#answerCurrentQuestion() will not throw an exception but rather //
					 * return null no matter what // - Akiwrapper#getCurrentQuestion() will also
					 * keep returning null
					 */
					// if (toRetStr != null && toRetStr.length() < 1) { //condition not required
					deDupeGuessPool(userCode);
					List<Guess> currGuessesForUser = guessesPool.get(userCode);
					if (currGuessesForUser != null && !currGuessesForUser.isEmpty()) {
						String userGuessesStr = "";
						for (Guess currGuess : currGuessesForUser) {
							userGuessesStr += "@@@" + currGuess.getName() + "~" + currGuess.getDescription() + "~"
									+ currGuess.getImage() + "~" + currGuess.getProbability();
						}
						toRetStr = userGuessesStr + "--akinatorlostwithguesses";
					} else {
						toRetStr = "akinatorlostwithoutguesses";
					}
					optimisePools(userCode);
					// }
				} else {
					toRetStr = (q.getStep() + 1) + ". " + q.getQuestion();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			toRetStr = errorHandle(e, userCode);
		}

		return toRetStr;
	}

	public void deDupeGuessPool(String userCode) {
		List<Guess> listOfGuessesForUser = guessesPool.get(userCode);
		List<Guess> deDupedGuesses = new ArrayList<>();
		for (Guess currGuess : listOfGuessesForUser) {
			if (!declinedGuesses.get(userCode).contains(Long.valueOf(currGuess.getIdLong()))) {
				if (guessIdNotIn(currGuess, deDupedGuesses)) {
					deDupedGuesses.add(currGuess);
				}
			}
		}
		if (deDupedGuesses != null && !deDupedGuesses.isEmpty()) {
			Collections.reverse(deDupedGuesses);
		}
		guessesPool.put(userCode, deDupedGuesses);
	}

	public boolean guessIdNotIn(Guess guessToChk, List<Guess> listToChk) {
		boolean toRet = true;
		for (Guess currGuessToChk : listToChk) {
			if (currGuessToChk.getId().equals(guessToChk.getId())) {
				toRet = false;
			}
		}
		return toRet;
	}

	public void saveGuesses(Akiwrapper currentAw, String userCode) {
		List<Guess> listOfGuessesForUser = guessesPool.get(userCode);
		List<Guess> currGuesses;
		try {
			currGuesses = currentAw.getGuesses();
			for (Guess currGuess : currGuesses) {
				if (!listOfGuessesForUser.contains(currGuess)) {
					if (!declinedGuesses.get(userCode).contains(Long.valueOf(currGuess.getIdLong()))) {
						if (currGuess.getProbability() > 0.3) {
							listOfGuessesForUser.add(currGuess);
						}
					}
				}
			}
			guessesPool.put(userCode, listOfGuessesForUser);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String errorHandle(Exception e, String userCode) {
		e.printStackTrace();
		optimisePools(userCode);
		return errorPrefix + "Session Timed out, please Start Again";
	}

	public void optimisePools(String userCode) {
		if (apiPool != null && !apiPool.isEmpty()) {
			apiPool.remove(userCode);
		}
		if (declinedGuesses != null && !declinedGuesses.isEmpty()) {
			declinedGuesses.remove(userCode);
		}
		if (guessesPool != null && !guessesPool.isEmpty()) {
			guessesPool.remove(userCode);
		}
	}
}