package com.zhiyou100.log;

import org.aspectj.lang.JoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zhiyou100.entity.AccountDO;

public class Log4jAspect {

	Logger logger = LoggerFactory.getLogger(Log4jAspect.class);
	
	
	// JoinPoint 可以获取切入点的所有信息
	public void accountAfterReturning(JoinPoint joinPoint) {

		// getArgs 可以获取切入点方法的所有参数，通过索引获取某一个
		AccountDO accountDO = (AccountDO) joinPoint.getArgs()[0];
		
		String method = joinPoint.getSignature().getName();
		
		logger.info("======" + accountDO.getEmail() + "======" + method);
	}
	
	// ex 要 aop 配置中的 throwing 保持一致，表示方法抛出的异常
	public void accountAfterThrowing(JoinPoint joinPoint, Exception ex) {

		AccountDO accountDO = (AccountDO) joinPoint.getArgs()[0];
		
		String method = joinPoint.getSignature().getName();

		logger.info("======" + accountDO.getEmail() + "======" + ex + "======" + method);
	}
}
