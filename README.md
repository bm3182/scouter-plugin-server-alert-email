# scouter-plugin-server-alert-email
### Scouter server plugin to send a alert via email

- 본 프로젝트는 스카우터 서버 플러그인으로써 서버에서 발생한 Alert 메시지를 Email로 발송하는 역할을 한다.
- 현재 지원되는 Alert의 종류는 다음과 같다.
	- Agent의 CPU (warning / fatal)
	- Agent의 Memory (warning / fatal)
	- Agent의 Disk (warning / fatal)
	- 신규 Agent 연결
	- Agent의 연결 해제
	- Agent의 재접속
	- 응답시간의 임계치 초과
	- GC Time의 임계치 초과
	- Thread 갯수의 임계치 초과

### Properties (스카우터 서버 설치 경로 하위의 conf/scouter.conf)
* **_ext\_plugin\_email\_send_alert_** : Email 발송 여부 (true / false) - 기본 값은 false
* **_ext\_plugin\_email\_debug_** : 로깅 여부 - 기본 값은 false
* **_ext\_plugin\_email\_level_** : 수신 레벨(0 : INFO, 1 : WARN, 2 : ERROR, 3 : FATAL) - 기본 값은 0
* **_ext\_plugin\_email\_smtp_hostname_** : SMTP 서버의 IP 또는 Domain - 기본 값은 smtp.gmail.com
* **_ext\_plugin\_email\_smtp_port_** : SMTP Port - 기본 값은 587
* **_ext\_plugin\_email\_smtpauth_enabled_** : SMTP 인증 사용 여부 - 기본 값은 true
* **_ext\_plugin\_email\_username_** : Email 사용자 계정
* **_ext\_plugin\_email\_password_** : Email 사용자 비밀번호
* **_ext\_plugin\_email\_ssl_enabled_** : SSL 사용 여부 - 기본 값은 true
* **_ext\_plugin\_email\_starttls_enabled_** : STARTTLS 사용 여부 - 기본 값은 true
* **_ext\_plugin\_email\_from_address_** : Email 발신자 계정
* **_ext\_plugin\_email\_to_address_** : Email 수신 계정(다중 사용자 지정 시 ',' 구분자 사용)
* **_ext\_plugin\_email\_cc_address_** : Email 참조 수신 계정(다중 사용자 지정 시 ',' 구분자 사용)
* **_ext\_plugin\_elapsed\_time_threshold_** : 응답시간의 임계치 (ms) - 기본 값은 0으로, 0일때 응답시간의 임계치 초과 여부를 확인하지 않는다.
* **_ext\_plugin\_gc\_time_threshold_** : GC Time의 임계치 (ms) - 기본 값은 0으로, 0일때 GC Time의 임계치 초과 여부를 확인하지 않는다.
* **_ext\_plugin\_thread\_count_threshold_** : Thread Count의 임계치 - 기본 값은 0으로, 0일때 Thread Count의 임계치 초과 여부를 확인하지 않는다.
* **_ext\_plugin\_ignore\_name_patterns_** : Alert 메시지 발송에서 제외할 NAME 패턴 목록 (',' 구분자 사용, * (wildcard) 사용 가능)
* **_ext\_plugin\_ignore\_title_patterns_** : Alert 메시지 발송에서 제외할 TITLE 패턴 목록 (',' 구분자 사용, * (wildcard) 사용 가능)
* **_ext\_plugin\_ignore\_message_patterns_** : Alert 메시지 발송에서 제외할 MESSAGE 패턴 목록 (',' 구분자 사용, * (wildcard) 사용 가능)
* **_ext\_plugin\_ignore\_continuous_dup_alert_** : 연속된 동일 Alert을 1시간 동안 제외 - 기본 값은 false

* Example
```
# Heap Used의 임계치 (M) - 기본 값은 0으로, 0일때 Heap Used의 임계치 초과 여부를 확인하지 않는다.
ext_plugin_heap_used_threshold=2000
ext_plugin_4G_heap_used_threshold=4000
ext_plugin_6G_heap_used_threshold=6000
ext_plugin_8G_heap_used_threshold=7800
# Heap Used의 임계치 적용 대상 서버
ext_plugin_heap_8g_servers=/gprtwas1/wise_prd11,/gprtwas1/wise_prd12,/gprtwas2/wise_prd21,/gprtwas2/wise_prd22
ext_plugin_heap_6g_servers=/pEacA1/PFLS_LIVE1,/pEacA2/PFLS_LIVE2,/cjwas03/qmswas1,/cjwas04/qmswas2
ext_plugin_heap_4g_servers=/cjwas03/expwas01,/cjwas04/expwas02,/cjwas03/amsprd_1,/cjwas04/amsprd_2,/cjwas03/cmsprd_1,/cjwas04/cmsprd_2,/cjirisap1/bmis_was1,/cjirisap1/iris_was1,/cjemap/bmis_was2,/cjemap/iris_was2

# External Interface (Email)
# Email 발송 여부 (true / false) - 기본 값은 false
ext_plugin_email_send_alert=true
# xlog exception alert - 기본 값은 false
ext_plugin_exception_xlog_email_enabled=true
# xlog exception alert 시스템별 - 기본 값은 false
ext_plugin_exception_xlog_mpro_email_enabled=true
ext_plugin_exception_xlog_exp_email_enabled=true
ext_plugin_exception_xlog_tms_email_enabled=true
ext_plugin_exception_xlog_ods_email_enabled=true
ext_plugin_exception_xlog_ams_email_enabled=true
ext_plugin_exception_xlog_wings_email_enabled=true
ext_plugin_exception_xlog_hanaro_email_enabled=true
ext_plugin_exception_xlog_fta_email_enabled=true
ext_plugin_exception_xlog_qms_email_enabled=true
ext_plugin_exception_xlog_cis_email_enabled=true
ext_plugin_exception_xlog_igap_email_enabled=true
ext_plugin_exception_xlog_esc_email_enabled=true
# wise는 xlog 알림 false
ext_plugin_exception_xlog_wise_email_enabled=true
ext_plugin_exception_xlog_bmis_email_enabled=true
ext_plugin_exception_xlog_iris_email_enabled=true
ext_plugin_exception_xlog_cms_email_enabled=true
ext_plugin_exception_xlog_pfls_email_enabled=true
ext_plugin_exception_xlog_meta_email_enabled=true

# 로깅 여부 - 기본 값은 false
ext_plugin_email_debug=true
# 수신 레벨(0 : INFO, 1 : WARN, 2 : ERROR, 3 : FATAL) - 기본 값은 0
ext_plugin_email_level=2
ext_plugin_email_smtp_hostname=escmail1.cj.net
ext_plugin_email_smtp_port=25
ext_plugin_email_smtpauth_enabled=false
#ext_plugin_email_username=
#ext_plugin_email_password=
ext_plugin_email_ssl_enabled=false
ext_plugin_email_starttls_enabled=true
ext_plugin_email_from_address=scouter@apm.net
# email_to by [,]로 다중 지정
ext_plugin_email_to_address=
ext_plugin_email_cc_address=cjcj_onsleader@cjintra.net
ext_plugin_email_bcc_address=kisu.jung@cj.net

# 시스템별 담당자 지정 [,]로 다중 지정
#Tradeone : 유현모님, 이규호님, 이탁희님
ext_plugin_email_mpro_address=hm.yu1@cj.net,kyuho.lee3@cj.net,gwangjin.lee@cj.net
#수출시스템 : 이광진님, 박수경님, 이영훈님
ext_plugin_email_exp_address=gwangjin.lee@cj.net,sukyung.park3@cj.net,younghoon.lee@cj.net
#온도관제 : 이광진님, 이종수님, 김순화님, 박수경님, 이영훈님
ext_plugin_email_tms_address=gwangjin.lee@cj.net,jongsoo.lee6@cj.net,sh.kim45@cj.net,sukyung.park3@cj.net,younghoon.lee@cj.net
#ODS : 이광진님, 이재경님, 박수경님, 이영훈님
ext_plugin_email_ods_address=gwangjin.lee@cj.net,jaekyung.lee2@cj.net,sukyung.park3@cj.net,younghoon.lee@cj.net
#ACMS : 이광진님, 이재경님, 박수경님, 이영훈님
ext_plugin_email_ams_address=gwangjin.lee@cj.net,jaekyung.lee2@cj.net,sukyung.park3@cj.net,younghoon.lee@cj.net
#대리점 : 이광진님, 이종수님, 김순화님, 박수경님, 이영훈님
ext_plugin_email_wings_address=gwangjin.lee@cj.net,jongsoo.lee6@cj.net,sh.kim45@cj.net,sukyung.park3@cj.net,younghoon.lee@cj.net
#하나로 : 이광진님, 이종수님, 김순화님, 박수경님, 이영훈님
ext_plugin_email_hanaro_address=gwangjin.lee@cj.net,jongsoo.lee6@cj.net,sh.kim45@cj.net,sukyung.park3@cj.net,younghoon.lee@cj.net
#FTA : 이광진님, 박주일님, 정민지님
ext_plugin_email_fta_address=gwangjin.lee@cj.net,juil.park@cj.net,mj.jeong2@cj.net
#QMS : 남형주님, 이탁희님, 신민철님
ext_plugin_email_qms_address=hj.nam3@cj.net,th.lee19@cj.net,mincheol.shin2@cj.net
#CIS : 신효진님, 이탁희님
ext_plugin_email_cis_address=hj.shin24@cj.net,th.lee19@cj.net
#통합결재 : 이수미님, 서지윤님, 이상엽님
ext_plugin_email_igap_address=soomi.lee@cj.net,jiyoon.seo@cj.net,sangyeup.lee@cj.net
#전자인장계약 : 이수미님, 이상엽님, 박가람님
ext_plugin_email_esc_address=soomi.lee@cj.net,sangyeup.lee@cj.net,garam.park5@cj.net
#WISE포탈 : 이수미님, 유명선님, 서종호님, 최우석님
ext_plugin_email_wise_address=wise-soomi.lee@cj.net,wise-myeongseon.ryu@cj.net,wise-jongho.seo1@cj.net,wise-ws.choi9@cj.net
#BIMS : 이수미님, 유명선님, 최우석님, 윤희선님
ext_plugin_email_bmis_address=soomi.lee@cj.net,myeongseon.ryu@cj.net,ws.choi9@cj.net,heesun.yoon@cj.net
#IRIS : 이수미님,유명선님, 최우석님, 윤희선님
ext_plugin_email_iris_address=soomi.lee@cj.net,myeongseon.ryu@cj.net,ws.choi9@cj.net,heesun.yoon@cj.net
#CMS : 이수미님, 서지윤님, 이상엽님, 박가람님, 배정관님, 송효주님, 양효승님, 김소미님
ext_plugin_email_cms_address=soomi.lee@cj.net,jiyoon.seo@cj.net,sangyeup.lee@cj.net,garam.park5@cj.net,jk.bae@cj.net,hyoju.song@cj.net,hyoseung.yang@cj.net,somi.kim3@cj.net
#Live : 윤현민님, 김용현님, 윤희선님, 유명선님, 유재승님
ext_plugin_email_pfls_address=yhm0402@cj.net,yh0219@cj.net,heesun.yoon@cj.net,myeongseon.ryu@cj.net,jaeseung.you@cj.net
#메타검색 : 유재승님
ext_plugin_email_meta_address=jaeseung.you@cj.net

# Alert 메시지 발송에서 제외할 NAME 패턴 목록 (',' 구분자 사용, * (wildcard) 사용 가능)
ext_plugin_ignore_email_name_patterns=
# Alert 메시지 발송에서 제외할 LEVEL 패턴 목록 (',' 구분자 사용, * (wildcard) 사용 가능)
ext_plugin_ignore_email_level_patterns=
# Alert 메시지 발송에서 제외할 TITLE 패턴 목록 (',' 구분자 사용, * (wildcard) 사용 가능)
ext_plugin_ignore_email_title_patterns=Elapsed,CONNECTION,activat*
# Alert 메시지 발송에서 제외할 MESSAGE 패턴 목록 (',' 구분자 사용, * (wildcard) 사용 가능)
ext_plugin_ignore_email_message_patterns=/theme/cheiljedang/summary/dashboard*,/common/bridge*,/theme/cheiljedang/main/addMember*,*/errorPage/page_not_found*,*warning slow sql*,*UserHandleException*
# 연속된 동일 Alert을 1시간 동안 제외 - 기본 값은 false
ext_plugin_ignore_email_continuous_dup_alert=true
```

### Dependencies
* Project
    - scouter.common
    - scouter.server
* Library
    - activation-1.1.1.jar
    - commons-email-1.4.jar
    - javax.mail-1.5.2.jar
    
### Build & Deploy
* Build
    - mvn clean package를 실행한다.
    
* Deploy
    - Maven 빌드 후 target/lib 디렉토리의 디펜던시 라이브러리와 함께 scouter-plugin-server-alert-email-1.0.0.jar 파일을 복사하여 스카우터 서버 설치 경로 하위의 lib/ 폴더에 저장한다.
