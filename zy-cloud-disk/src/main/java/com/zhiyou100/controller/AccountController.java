package com.zhiyou100.controller;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.zhiyou100.entity.AccountDO;
import com.zhiyou100.service.AccountService;
import com.zhiyou100.vo.ResponseVO;

@CrossOrigin
@RestController
public class AccountController {

	@Autowired
	private AccountService service;
	
	@RequestMapping(value = "/session", method = RequestMethod.POST)
	public ResponseVO<String> login(AccountDO accountDO, HttpSession session) {

		accountDO = service.login(accountDO);
		
		session.setAttribute("accountId", accountDO.getId());

		return new ResponseVO<String>("登录成功");
	}
	
	@RequestMapping(value = "/accounts", method = RequestMethod.POST)
	public ResponseVO<String> register(AccountDO accountDO, HttpSession session) {

		accountDO = service.register(accountDO);
		
		session.setAttribute("accountId", accountDO.getId());
		
		return new ResponseVO<String>("注册成功");
	}
}
