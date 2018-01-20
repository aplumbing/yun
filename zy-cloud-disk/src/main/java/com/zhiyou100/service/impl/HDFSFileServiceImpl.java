package com.zhiyou100.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.zhiyou100.dao.DirectoryDao;
import com.zhiyou100.dao.FileDao;
import com.zhiyou100.entity.DirectoryDO;
import com.zhiyou100.entity.FileDO;
import com.zhiyou100.exception.DirectoryNameExistsException;
import com.zhiyou100.exception.DownloadFileException;
import com.zhiyou100.exception.UploadFileException;
import com.zhiyou100.service.FileService;

public class HDFSFileServiceImpl implements FileService {

	private String localDirPath;
	private String hdfsDIrPath;

	private FileDao fileDao;

	private DirectoryDao directoryDao;

	@Override
	public DirectoryDO upload(MultipartFile file, Long accountId, Long directoryId) {

		try {
			// md5 加密文件 xxxxx.type
			String originalName = file.getOriginalFilename();
			String type = originalName.substring(originalName.lastIndexOf("."));
			String uplaodFileName = DigestUtils.md5DigestAsHex(file.getBytes()) + type;

			// 文件上传后的路径
			String hdfsFilePath = hdfsDIrPath + uplaodFileName;

			// 把上传的文件添加到file and directory
			FileDO fileDO = new FileDO(uplaodFileName, hdfsFilePath);
			int fileAddCOunt = fileDao.addFile(fileDO);

			DirectoryDO directoryDO = new DirectoryDO(accountId, originalName, directoryId, true, fileDO.getId());
			int diretoryAddCount = directoryDao.addDirectory(directoryDO);

			if (diretoryAddCount == 0) {
				throw new DirectoryNameExistsException();
			}

			if (fileAddCOunt != 0) {
				// 开始上传本地文件
				// 先上传本地目录

				String localFilePath = localDirPath + uplaodFileName;
				File localFile = new File(localFilePath);
				// 上传文件
				file.transferTo(localFile);

				// 然后上传到hdfs
				Configuration conf = new Configuration();
				conf.set("fs.defaultFS", "hdfs://master:9000");
				try (FileSystem fs = FileSystem.get(conf);) {

					Path hdp = new Path(hdfsDIrPath);
					if (!fs.exists(hdp)) {
						fs.mkdirs(hdp);
					}
					Path src = new Path(localFilePath);
					Path dst = new Path(hdfsFilePath);
					fs.copyFromLocalFile(src, dst);

					localFile.delete();
				}

			}

			return directoryDO;
		} catch (IOException e) {
			e.printStackTrace();
			throw new UploadFileException();
		}

	}

	@Override
	public byte[] download(Long fileId) {

		String hdfsFilePath = fileDao.getFilePathById(fileId);
		String localFilePath = localDirPath + "temp.eee";

		Configuration conf = new Configuration();
		conf.set("fs.defaultFS", "hdfs://master:9000");
		try (FileSystem fs = FileSystem.get(conf);) {
			Path src = new Path(hdfsFilePath);
			Path dst = new Path(localFilePath);
			fs.copyToLocalFile(src, dst);

			File localFile = new File(localFilePath);

			try (FileInputStream fileInputStream = new FileInputStream(localFile);) {
				byte[] data = new byte[fileInputStream.available()];
				fileInputStream.read(data);

				// 从本地删除文件
				localFile.delete();
				return data;
			}

		} catch (IOException e) {
			e.printStackTrace();
			throw new DownloadFileException();
		}

	}

	public String getLocalDirPath() {
		return localDirPath;
	}

	public void setLocalDirPath(String localDirPath) {
		this.localDirPath = localDirPath;
	}

	public String getHdfsDIrPath() {
		return hdfsDIrPath;
	}

	public void setHdfsDIrPath(String hdfsDIrPath) {
		this.hdfsDIrPath = hdfsDIrPath;
	}

	public FileDao getFileDao() {
		return fileDao;
	}

	public void setFileDao(FileDao fileDao) {
		this.fileDao = fileDao;
	}

	public DirectoryDao getDirectoryDao() {
		return directoryDao;
	}

	public void setDirectoryDao(DirectoryDao directoryDao) {
		this.directoryDao = directoryDao;
	}

}
