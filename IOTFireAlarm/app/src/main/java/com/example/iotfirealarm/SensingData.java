/*
-작성자: 2017038023 반예린
-해당 소스파일 정보: JSON 파싱을 위한 데이터 구조.
-테스트 환경: Nexus 5X(AVD), API 29
 */

package com.example.iotfirealarm;

import java.util.List;

public class SensingData {
    ChannelInfo channel;
    List<SensingDataItem> feeds;
}
