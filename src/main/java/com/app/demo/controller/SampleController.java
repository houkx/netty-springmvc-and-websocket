package com.app.demo.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.demo.CDPlayer;

@Controller
public class SampleController {
	@Autowired
	private CDPlayer player;

	@RequestMapping("/")
	String home() {
		return "ws";
	}

	@RequestMapping("/h")
	@ResponseBody
	String hi(HttpServletRequest req) {
		System.out.println("session = " + req.getSession());
		player.play();
		return "Hello World! I am netty-springmvc !";
	}

	@RequestMapping("/r")
	String testRedirect() {
		return "redirect:/rdnew";
	}

	@RequestMapping("/rdnew")
	String rdnew() {
		return "rd";
	}
}
