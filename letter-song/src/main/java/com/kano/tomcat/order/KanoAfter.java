package com.kano.tomcat.order;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;

/**
 * 先创建
 * @see KanoBefore
 */
@AutoConfigureAfter(KanoBefore.class)
public class KanoAfter {
	KanoAfter(){
	}
}
