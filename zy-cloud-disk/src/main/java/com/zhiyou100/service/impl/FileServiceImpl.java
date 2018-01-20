package com.zhiyou100.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.transaction.annotation.Transactional;
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

// 不适用 service 注解，在 spring-service.xml 通过 bean 进行注册
// 需要为每个属性增加 setter 方法，才能进行属性注入
public class FileServiceImpl implements FileService {

	// 文件上传后保存的目录：/xxx/yyy/ 必须以 / 结尾
	private String localDirPath;
	private String hdfsDirPath;
	
	private DirectoryDao directoryDao;
	
	private FileDao fileDao;
	
	// 使用事务保证所有的操作都成功，任何一个地方出错就回滚到最初的状态
	@Transactional
	public DirectoryDO upload(MultipartFile file, Long accountId, Long directoryId) {

		try {
			
			// 获取文件的原始名字：xxxx.jpg xxxx.mp4
			String originalName = file.getOriginalFilename();
				
			String type = originalName.substring(originalName.lastIndexOf("."));
			
			// 获取文件 md5 加密后的结果 + 后缀名作为文件上传后名字
			String uploadFileName = DigestUtils.md5DigestAsHex(file.getBytes()) + type;
			
			// 文件上传 hdfs 后保存的路径
			String hdfsFilePath = hdfsDirPath + uploadFileName;
			
			// 把上传的文件信息插入到 file 表
			FileDO fileDO = new FileDO(uploadFileName, hdfsFilePath); 
			
			int fileAddCount = fileDao.addFile(fileDO);

			// 把上传的文件信息插入到 directory 表
			DirectoryDO directoryDO = new DirectoryDO(accountId, originalName, directoryId, true, fileDO.getId());
			
			int directoryAddCount = directoryDao.addDirectory(directoryDO);
			
			// 插入失败，说明以有同名的文件了，不能上传
			if (directoryAddCount == 0) {
				
				throw new DirectoryNameExistsException();
			}
			
			// 插入失败，说明已经上传过了，不用上传
			if (fileAddCount != 0) {
				
				// 开始上传文件
				
				// 先上传到本地目录
				String localFilePath = localDirPath + uploadFileName;
				File localFile = new File(localFilePath);
				
				file.transferTo(localFile);
				
				// 然后从本地上传到 hdfs
				
				Configuration conf = new Configuration();
				
				conf.set("fs.defaultFS", "hdfs://master:9000");
				
				try (FileSystem fs = FileSystem.get(conf);){
					
					// 先判断文件夹是否存在
					Path hdp= new Path(hdfsDirPath);
					
					if (!fs.exists(hdp)) {
						
						// 如果文件夹不存在，创建
						fs.mkdirs(hdp);
					}
					
					Path src = new Path(localFilePath);
					Path dst = new Path(hdfsFilePath);
					
					fs.copyFromLocalFile(src, dst);
					
					// 上传完毕，删除本地的文件
					localFile.delete();
				}
			}
			
			return directoryDO;
		} catch (IOException e) {

			e.printStackTrace();
			
			throw new UploadFileException();
		}
	}

	public byte[] download(Long fileId) {
		
		String hdfsFilePath = fileDao.getFilePathById(fileId);
		String localFilePath = localDirPath + "temp.tttt";
		
		
		// 先从 hdfs 下载到本地
		Configuration conf = new Configuration();
		
		conf.set("fs.defaultFS", "hdfs://master:9000");
		
		try (FileSystem fs = FileSystem.get(conf);){
			
			Path src = new Path(hdfsFilePath);
			Path dst = new Path(localFilePath);
			
			fs.copyToLocalFile(src, dst);
			
			// 然后再从本地发送给客户端
			File localFiel = new File(localFilePath);
			
			try (FileInputStream fileInputStream = new FileInputStream(localFiel);){
				
				byte[] data = new byte[fileInputStream.available()];
				
				fileInputStream.read(data);
				
				// 从本地删除 tem.tttt
				localFiel.delete();
				
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

	public String getHdfsDirPath() {
		return hdfsDirPath;
	}

	public void setHdfsDirPath(String hdfsDirPath) {
		this.hdfsDirPath = hdfsDirPath;
	}

	public DirectoryDao getDirectoryDao() {
		return directoryDao;
	}

	public void setDirectoryDao(DirectoryDao directoryDao) {
		this.directoryDao = directoryDao;
	}

	public FileDao getFileDao() {
		return fileDao;
	}

	public void setFileDao(FileDao fileDao) {
		this.fileDao = fileDao;
	}
}
