package com.iopts.scheduler.queue;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.skyun.app.util.config.AppConfig;
import com.skyun.recon.util.database.ibatis.vo.pathVo;

public class QueueStaticPool {

	static BlockingQueue<pathVo> JobQueue = null;
	static HashMap<String,String> exception_hash = null;

	static {
		JobQueue = new ArrayBlockingQueue<pathVo>(AppConfig.getPropertyInt("config.queue.size"));
		exception_hash=new HashMap<>();
	}

	public BlockingQueue<pathVo> getJobQueue() {
		return JobQueue;
	}

	public void setJobQueue(BlockingQueue<pathVo> jobQueue) {
		JobQueue = jobQueue;
	}

	public static HashMap<String, String> getException_hash() {
		return exception_hash;
	}

	public static void setException_hash(HashMap<String, String> h) {
		exception_hash.clear();
		exception_hash = h;
	}
	

}
