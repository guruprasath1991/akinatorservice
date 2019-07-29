package com.erixatech.akinatorservice.controller;

import java.util.ArrayList;
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

	@RequestMapping("/start")
	public String start(@RequestParam String userCode, @RequestParam String userLang) {
		String toRetStr = "";
		try {

			Akiwrapper aw = new AkiwrapperBuilder().setFilterProfanity(true)
					.setLocalization(getUserLanguage(userLang.toLowerCase().trim())).build();
			apiPool.put(userCode, aw);
			declinedGuesses.put(userCode, new ArrayList<>());
			Question q = aw.getCurrentQuestion();
			toRetStr = (q.getStep() + 1) + ". " + q.getQuestion();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			toRetStr = errorPrefix + "Could not Contact Server. Check your Internet connection and Try Again";
		}
		return toRetStr;
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
			if (!isGuessMatches) {
				Question q = currentAw.getCurrentQuestion();
				if (q == null) {
					// Watch out! Akinator has ran out of questions!
					// In this case,
					// - Akiwrapper#answerCurrentQuestion() will not throw an exception but rather
					// return null no matter what
					// - Akiwrapper#getCurrentQuestion() will also keep returning null
					List<Guess> allGuesses = currentAw.getGuesses();
					for (Guess currGuess : allGuesses) {
						toRetStr += "@@@" + currGuess.getName() + "~" + currGuess.getDescription() + "~" + currGuess.getImage()
								+ "~" + currGuess.getProbability();
					}
					if (allGuesses != null && allGuesses.size() > 0) {
						toRetStr += "--akinatorlostwithguesses";
					}
					apiPool.remove(userCode);
				} else {
					toRetStr = (q.getStep() + 1) + ". " + q.getQuestion();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			toRetStr = errorHandle(e, userCode);
		}
		if (toRetStr != null && toRetStr.length() < 1) {
			toRetStr = "akinatorlostwithoutguesses";
			apiPool.remove(userCode);
		}
		return toRetStr;
	}

	public String errorHandle(Exception e, String userCode) {
		e.printStackTrace();
		apiPool.remove(userCode);
		return errorPrefix + "Session Timed out, please Start Again";
	}
}