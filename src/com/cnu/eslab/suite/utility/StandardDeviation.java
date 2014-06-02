package com.cnu.eslab.suite.utility;

import java.util.ArrayList;

/*
 * 평균, 최대값, 최소값, 표준 편차를 계산 해주는 유틸리티 클래스 이다. 
 */
public class StandardDeviation {

	public final static int DOUBLE = 0;  
	private ArrayList<Double> mItemList;
	
	public StandardDeviation(int mode){
		if(mode == DOUBLE){
			mItemList = new ArrayList<Double>();
		}
		else{
			
		}
	}
	public boolean add(double arItem){
		mItemList.add(arItem);
		return true;
	}
	//표준편차 계싼
	public double EvaluateSD(){
		double sd = 0.0;
		double sum = 0.0;
		double pow2sum = 0.0;
		double avg = 0.0;
		double pow2avg = 0.0;
		for(int i = 0; i<mItemList.size(); i++){
			sum += mItemList.get(i);	// 그냥합
			pow2sum += Math.pow(mItemList.get(i),2); // 제곱의 합
		}
		
		//평균 계산
		avg = sum/mItemList.size();
		pow2avg = pow2sum/mItemList.size();
		
		//표준편차 계산. 식 : 제곱평균 - 평균의 제곱 의 루트
		sd = Math.sqrt(pow2avg - Math.pow(avg, 2));
		
		return sd;
	}
	//평균 계산
	public double EvaluateAVG(){
		double avg = 0.0;
		double sum = 0.0;
		for(int i = 0; i<mItemList.size(); i++){
			sum += mItemList.get(i);	// 그냥합
			
		}
		avg = sum / mItemList.size();
		return avg;
	}
	//최대 값을 계산해 준다.
	public double EvaluateMAX(){
		double max = mItemList.get(0);
		for(int i = 0; i<mItemList.size(); i++){
			if(mItemList.get(i) > max){
				max = mItemList.get(i);
			}
		}
		return max;
	}
	//최소 값을 계산 한다.
	public double EvaluateMIN(){
		double min = mItemList.get(0);
		for(int i = 0; i<mItemList.size(); i++){
			if(mItemList.get(i) < min){
				min = mItemList.get(i);
			}
		}
		return min;
	}
	//초기화를 한다.
	public boolean clear(){
		mItemList.clear();
		return true;
	}
}
