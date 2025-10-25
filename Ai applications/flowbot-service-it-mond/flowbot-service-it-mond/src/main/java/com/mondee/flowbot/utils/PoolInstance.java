package com.mondee.flowbot.utils;

import java.time.Instant;

public class PoolInstance {
		String s;
		int c;
		long date;
		public PoolInstance(String s) {
			this.s=s;
			this.c=0;
			this.date=Instant.now().getEpochSecond();
		}
		public void inc() {
			this.c++;
			this.date=Instant.now().getEpochSecond();
		}
		public String getS() {
			return s;
		}
		public void setS(String s) {
			this.s = s;
		}
		
		public int getC() {
			return c;
		}
		public void setC(int c) {
			this.c = c;
		}
		public long getDate() {
			return date;
		}
		public void setDate(long date) {
			this.date = date;
		}

}
