package com.zhiyou100.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;

import com.zhiyou100.entity.DirectoryDO;
import com.zhiyou100.service.FileService;
import com.zhiyou100.vo.ResponseVO;

@CrossOrigin
@RestController
public class FileController {

	@Autowired
	private FileService service;

	@RequestMapping(value = "/files", method = RequestMethod.POST)
	public ResponseVO<DirectoryDO> uploadFile(MultipartFile file, Long directoryId, @SessionAttribute Long accountId) {

		DirectoryDO directoryDO = service.upload(file, accountId, directoryId);

		return new ResponseVO<>(directoryDO);
	}

	@RequestMapping("/files/{fileId}")
	public ResponseEntity<byte[]> downloadFile(@PathVariable Long fileId, String fileName) throws UnsupportedEncodingException {

		byte[] body = service.download(fileId);

		HttpHeaders headers = new HttpHeaders();

		headers.add("Content-Disposition", "attchement;filename=" + URLEncoder.encode(fileName, "UTF-8"));

		HttpStatus statusCode = HttpStatus.OK;

		ResponseEntity<byte[]> entity = new ResponseEntity<byte[]>(body, headers, statusCode);

		return entity;
	}
}
