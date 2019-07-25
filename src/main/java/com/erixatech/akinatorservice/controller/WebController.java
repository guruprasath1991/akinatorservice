package com.erixatech.akinatorservice.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.markozajc.akiwrapper.Akiwrapper;
import com.markozajc.akiwrapper.Akiwrapper.Answer;
import com.markozajc.akiwrapper.AkiwrapperBuilder;
import com.markozajc.akiwrapper.core.entities.Guess;
import com.markozajc.akiwrapper.core.entities.Question;


@RestController
public class WebController {

	Akiwrapper aw = new AkiwrapperBuilder().build();

	@RequestMapping("/start")
	public String start() {
		/*
		 * SampleResponse response = new SampleResponse(); response.setId(1);
		 * response.setMessage("Your name is "+name); return response;
		 */
		aw = new AkiwrapperBuilder().build();
		Question q = aw.getCurrentQuestion();
		return q.getQuestion();
	}

	@RequestMapping("/yes")
	public String answerYes() {
		String toRetStr = "default";
		try {
			aw.answerCurrentQuestion(Answer.YES);
			toRetStr = whatsNext();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toRetStr;
	}

	@RequestMapping("/no")
	public String answerNo() {
		String toRetStr = "default";
		try {
			aw.answerCurrentQuestion(Answer.NO);
			toRetStr = whatsNext();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toRetStr;
	}

	@RequestMapping("/dontknow")
	public String answerDontKnow() {
		String toRetStr = "default";
		try {
			aw.answerCurrentQuestion(Answer.DONT_KNOW);
			toRetStr = whatsNext();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toRetStr;
	}

	@RequestMapping("/probably")
	public String answerProbably() {
		String toRetStr = "default";
		try {
			aw.answerCurrentQuestion(Answer.PROBABLY);
			toRetStr = whatsNext();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toRetStr;
	}

	@RequestMapping("/probablynot")
	public String answerProbablyNot() {
		String toRetStr = "default";
		try {
			aw.answerCurrentQuestion(Answer.PROBABLY_NOT);
			toRetStr = whatsNext();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toRetStr;
	}
	
	@RequestMapping("/undo")
	public String undoPrevAnswer() {
		String toRetStr = "default";
		try {
			Question q = aw.undoAnswer();
			toRetStr = q!=null ? q.getQuestion() : whatsNext();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toRetStr;
	}

	public String whatsNext() {
		boolean isGuessMatches = false;
		String toRetStr = "";
		try {
			for (Guess guess : aw
					.getGuessesAboveProbability(0.85 /* you can specify your threshold between 0 and 1 */)) {
				// Do something with those guesses
				isGuessMatches = true;
				toRetStr += guess.getName() + "~" + guess.getDescription() + "~" + guess.getImage() + "~"
						+ guess.getProbability()+"*";
			}
			if (!isGuessMatches) {
				Question q = aw.getCurrentQuestion();
				if (q == null) {
					// Watch out! Akinator has ran out of questions!
					// In this case,
					// - Akiwrapper#answerCurrentQuestion() will not throw an exception but rather
					// return null no matter what
					// - Akiwrapper#getCurrentQuestion() will also keep returning null
					List<Guess> allGuesses = aw.getGuesses();
					for (Guess currGuess : allGuesses) {
						toRetStr += currGuess.getName() + "~" + currGuess.getDescription() + "~" + currGuess.getImage()
								+ "~" + currGuess.getProbability()+"~akinatorlost*";
					}
				} else {
					toRetStr = q.getQuestion();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(toRetStr!=null && toRetStr.length()<1)
		{
			toRetStr = "akinatorlost";
		}
		return toRetStr;
	}
}