package com.kano.tomcat.condition;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("/kano")
@ResponseBody
@Controller
public class KanoController {
	@PostMapping("test")
	public String kano(){
		return "kano";
	}
}
