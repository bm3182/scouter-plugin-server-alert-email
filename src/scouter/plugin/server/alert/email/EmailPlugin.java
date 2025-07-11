/*
 *  Copyright 2016 Scouter Project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *  
 *  @author Sang-Cheon Park
 */
package scouter.plugin.server.alert.email;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

import scouter.lang.AlertLevel;
import scouter.lang.TextTypes;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.pack.XLogPack;
import scouter.lang.plugin.PluginConstants;
import scouter.lang.plugin.annotation.ServerPlugin;
import scouter.net.RequestCmd;
import scouter.server.Configure;
import scouter.server.CounterManager;
import scouter.server.Logger;
import scouter.server.core.AgentManager;
import scouter.server.db.TextRD;
import scouter.server.netio.AgentCall;
import scouter.util.DateUtil;
import scouter.util.HashUtil;

/**
 * Scouter server plugin to send alert via email
 * 
 * @author Sang-Cheon Park(nices96@gmail.com) on 2016. 3. 28.
 */
public class EmailPlugin {

    // Get singleton Configure instance from server
    final Configure conf = Configure.getInstance();

    private static AtomicInteger ai = new AtomicInteger(0);
    private static List<Integer> javaeeObjHashList = new ArrayList<Integer>();
    private static AlertPack lastPack;
    private static long lastSentTimestamp;

    public EmailPlugin() {
        if (ai.incrementAndGet() == 1) {
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

            // thread count check
            executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    for (int objHash : javaeeObjHashList) {
                        try {
                            if (AgentManager.isActive(objHash)) {
                                ObjectPack objectPack = AgentManager.getAgent(objHash);
                                MapPack mapPack = new MapPack();
                                mapPack.put("objHash", objHash);

                                mapPack = AgentCall.call(objectPack, RequestCmd.OBJECT_THREAD_LIST, mapPack);

                                int threadCountThreshold = conf.getInt("ext_plugin_thread_count_threshold", 0);
                                int threadCount = mapPack.getList("name").size();

                                if (threadCountThreshold != 0 && threadCount > threadCountThreshold) {
                                    AlertPack ap = new AlertPack();

                                    ap.level = AlertLevel.WARN;
                                    ap.objHash = objHash;
                                    ap.title = "Thread count exceed a threshold.";
                                    ap.message = objectPack.objName + "'s Thread count(" + threadCount + ") exceed a threshold.";
                                    ap.time = System.currentTimeMillis();
                                    ap.objType = objectPack.objType;

                                    alert(ap);
                                }
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }, 0, 5, TimeUnit.SECONDS);
        }
    }

    @ServerPlugin(PluginConstants.PLUGIN_SERVER_ALERT)
    public void alert(final AlertPack pack) {
        if (conf.getBoolean("ext_plugin_email_send_alert", false)) {

            // Get log level (0 : INFO, 1 : WARN, 2 : ERROR, 3 : FATAL)
            int level = conf.getInt("ext_plugin_email_level", 0);

            if (level <= pack.level) {
                new Thread() {
                    public void run() {
                        try {
                            // Get server configurations for email
                            String hostname = conf.getValue("ext_plugin_email_smtp_hostname", "smtp.gmail.com");
                            int port = conf.getInt("ext_plugin_email_smtp_port", 587);
                            boolean authEnabled = conf.getBoolean("ext_plugin_email_smtpauth_enabled", true);
                            String username = conf.getValue("ext_plugin_email_username");
                            String password = conf.getValue("ext_plugin_email_password");
                            boolean sslEnabled = conf.getBoolean("ext_plugin_email_ssl_enabled", true);
                            boolean tlsEnabled = conf.getBoolean("ext_plugin_email_starttls_enabled", true);
                            String from = conf.getValue("ext_plugin_email_from_address");
                            String to = conf.getValue("ext_plugin_email_to_address");
                            String cc = conf.getValue("ext_plugin_email_cc_address");
                            String bcc = conf.getValue("ext_plugin_email_bcc_address");

                            // Service Manager Mail
                            String esc_to = conf.getValue("ext_plugin_email_esc_address");
                            String exp_to = conf.getValue("ext_plugin_email_exp_address");
                            String tms_to = conf.getValue("ext_plugin_email_tms_address");
                            String igap_to = conf.getValue("ext_plugin_email_igap_address");
                            String wise_to = conf.getValue("ext_plugin_email_wise_address");
                            String mpro_to = conf.getValue("ext_plugin_email_mpro_address");
                            String cis_to = conf.getValue("ext_plugin_email_cis_address");
                            String ods_to = conf.getValue("ext_plugin_email_ods_address");
                            String cpl_to = conf.getValue("ext_plugin_email_cpl_address");
                            String qms_to = conf.getValue("ext_plugin_email_qms_address");
                            String meta_to = conf.getValue("ext_plugin_email_meta_address");
                            String bmis_to = conf.getValue("ext_plugin_email_bmis_address");
                            String iris_to = conf.getValue("ext_plugin_email_iris_address");
                            String pfls_to = conf.getValue("ext_plugin_email_pfls_address");
                            String ams_to = conf.getValue("ext_plugin_email_ams_address");
                            String cms_to = conf.getValue("ext_plugin_email_cms_address");
                            String fta_to = conf.getValue("ext_plugin_email_fta_address");
                            String hanaro_to = conf.getValue("ext_plugin_email_hanaro_address");

                            assert hostname != null;
                            assert port > 0;
                            assert username != null;
                            assert password != null;
                            assert from != null;
                            //assert to != null;

                            // Get agent Name
                            String name = AgentManager.getAgentName(pack.objHash) == null ? "N/A" : AgentManager.getAgentName(pack.objHash);

                            if (name.equals("N/A") && pack.message.endsWith("connected.")) {
                                int idx = pack.message.indexOf("connected");
                                if (pack.message.indexOf("reconnected") > -1) {
                                    name = pack.message.substring(0, idx - 6);
                                } else {
                                    name = pack.message.substring(0, idx - 4);
                                }
                            }

                            // Make email subject
                            String subject = "[" + AlertLevel.getName(pack.level) + "] " + pack.objType.toUpperCase() + "(" + name + ") : " + pack.title;

                            String title = pack.title;
                            String msg = pack.message;
                            if (title.equals("INACTIVE_OBJECT")) {
                                title = "An object has been inactivated.";
                                msg = pack.message.substring(0, pack.message.indexOf("OBJECT") - 1);
                            }

                            try {
                                String ignoreNamePattern = conf.getValue("ext_plugin_ignore_email_name_patterns");
                                String ignoreLevelPattern = conf.getValue("ext_plugin_ignore_email_level_patterns");
                                String ignoreTitlePattern = conf.getValue("ext_plugin_ignore_email_title_patterns");
                                String ignoreMessagePattern = conf.getValue("ext_plugin_ignore_email_message_patterns");

                                if (ignoreNamePattern != null && !"".equals(ignoreNamePattern)) {
                                    for (String pattern : ignoreNamePattern.split(",")) {
                                        if (name.matches(pattern.replaceAll("\\*", ".*"))) {
                                            return;
                                        }
                                    }
                                }

                                if (ignoreLevelPattern != null && !"".equals(ignoreLevelPattern)) {
                                    for (String pattern : ignoreLevelPattern.split(",")) {
                                        if (AlertLevel.getName(pack.level).matches(pattern.replaceAll("\\*", ".*"))) {
                                            return;
                                        }
                                    }
                                }

                                if (ignoreTitlePattern != null && !"".equals(ignoreTitlePattern)) {
                                    for (String pattern : ignoreTitlePattern.split(",")) {
                                        if (title.matches(pattern.replaceAll("\\*", ".*"))) {
                                            return;
                                        }
                                    }
                                }

                                if (ignoreMessagePattern != null && !"".equals(ignoreMessagePattern)) {
                                    for (String pattern : ignoreMessagePattern.split(",")) {
                                        if (msg.matches(pattern.replaceAll("\\*", ".*")
                                                .replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")
                                                .replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]"))) {
                                            return;
                                        }
                                    }
                                }

                                if (conf.getBoolean("ext_plugin_ignore_email_continuous_dup_alert", false) && lastPack != null) {
                                    long diff = System.currentTimeMillis() - lastSentTimestamp;
                                    if (lastPack.objHash == pack.objHash && lastPack.title.equals(pack.title) && diff < DateUtil.MILLIS_PER_HOUR) {
                                        return;
                                    }
                                }

                                lastPack = pack;
                            } catch (Exception e) {
                                // ignore
                                println("[Error] : " + e.getMessage());
                            }

                            // Make email message
                            String message =    "[TYPE] : " + pack.objType.toUpperCase() + "\n" + 
                                                "[NAME] : " + name + "\n" + 
                                                "[LEVEL] : " + AlertLevel.getName(pack.level) + "\n" + 
                                                "[TITLE] : " + title + "\n" + 
                                                "[MESSAGE] : " + msg;

                            // Create an Email instance
                            Email email = new SimpleEmail();

                            email.setHostName(hostname);
                            
                            if (sslEnabled) {
                                email.setSslSmtpPort(Integer.toString(port));
                            } else {
                                email.setSmtpPort(port);
                            }
                            
                            if (authEnabled) {
                                email.setAuthenticator(new DefaultAuthenticator(username, password));
                            }
                            email.setStartTLSEnabled(tlsEnabled);
                            email.setFrom(from);
                            email.setSubject(subject);
                            email.setMsg(message);

                            if ("/cjescwas01/escprd1".equals(name) || "/cjescwas02/escprd2".equals(name)) {
                                if (esc_to != null) {
                                    for (String addr : esc_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cjwas03/expwas01".equals(name) || "/cjwas04/expwas02".equals(name)) {
                                if (exp_to != null) {
                                    for (String addr : exp_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cjwas03/igap_was3".equals(name) || "/cjwas04/igap_was4".equals(name)) {
                                if (igap_to != null) {
                                    for (String addr : igap_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cjwas03/tmsprd1-1".equals(name) || "/cjwas03/tmsprd1-2".equals(name) || "/cjwas04/tmsprd2-1".equals(name) || "/cjwas04/tmsprd2-2".equals(name)) {
                                if (tms_to != null) {
                                    for (String addr : tms_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/gprtwas1/wise_prd11".equals(name) || "/gprtwas1/wise_prd12".equals(name) || "/gprtwas2/wise_prd21".equals(name) || "/gprtwas2/wise_prd22".equals(name)) {
                                if (wise_to != null) {
                                    for (String addr : wise_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cjwas03/mproWas03".equals(name) || "/cjwas04/mproWas04".equals(name)) {
                                if (mpro_to != null) {
                                    for (String addr : mpro_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cjwas01/cis1".equals(name) || "/cjwas02/cis2".equals(name)) {
                                if (cis_to != null) {
                                    for (String addr : cis_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cjodswas01/odsprd01".equals(name) || "/cjodswas02/odsprd02".equals(name)) {
                                if (ods_to != null) {
                                    for (String addr : ods_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cjpcplwas1/cplwas1".equals(name) || "/cjpcplwas2/cplwas2".equals(name)) {
                                if (cpl_to != null) {
                                    for (String addr : cpl_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cjwas03/qmswas1".equals(name) || "/cjwas04/qmswas2".equals(name)) {
                                if (qms_to != null) {
                                    for (String addr : qms_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cjirisap1/bmis_was1".equals(name) || "/cjemap/bmis_was2".equals(name)) {
                                if (bmis_to != null) {
                                    for (String addr : bmis_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cjirisap1/iris_was1".equals(name) || "/cjemap/iris_was2".equals(name)) {
                                if (iris_to != null) {
                                    for (String addr : iris_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/pEacA1/PFLS_LIVE1".equals(name) || "/pEacA2/PFLS_LIVE2".equals(name)) {
                                if (pfls_to != null) {
                                    for (String addr : pfls_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cjwas03/amsprd_1".equals(name) || "/cjwas04/amsprd_2".equals(name)) {
                                if (ams_to != null) {
                                    for (String addr : ams_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cjwas03/cmsprd_1".equals(name) || "/cjwas04/cmsprd_2".equals(name)) {
                                if (cms_to != null) {
                                    for (String addr : cms_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/CJHANAROWAS01/HANARO_PRD1".equals(name) || "/CJHANAROWAS02/HANARO_PRD2".equals(name)) {
                                if (cms_to != null) {
                                    for (String addr : hanaro_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/cj-meta-app/cj-meta-app".equals(name)) {
                                if (meta_to != null) {
                                    for (String addr : meta_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else if("/CJFTAAP/fta".equals(name)) {
                                if (fta_to != null) {
                                    for (String addr : fta_to.split(",")) {
                                        email.addTo(addr);
                                    }
                                }
                            } else {
                                for (String addr : to.split(",")) {
                                    email.addTo(addr);
                                }
                            }

                            if (cc != null) {
                                for (String addr : cc.split(",")) {
                                    email.addCc(addr);
                                }
                            }

                            if (bcc != null) {
                                for (String addr : bcc.split(",")) {
                                    email.addBcc(addr);
                                }
                            }

                            // Send the email
                            email.send();

                            lastSentTimestamp = System.currentTimeMillis();
                            println("Email sent to [" + name + "] [" + email.getToAddresses().toString() + "] successfully.");
                        } catch (Exception e) {
                            println("[Error] : " + e.getMessage());

                            if (conf._trace) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start();
            }
        }
    }

    @ServerPlugin(PluginConstants.PLUGIN_SERVER_OBJECT)
    public void object(ObjectPack pack) {
        if (pack.version != null && pack.version.length() > 0) {
            AlertPack ap = null;
            ObjectPack op = AgentManager.getAgent(pack.objHash);

            if (op == null && pack.wakeup == 0L) {
                // in case of new agent connected
                ap = new AlertPack();
                ap.level = AlertLevel.INFO;
                ap.objHash = pack.objHash;
                ap.title = "An object has been activated.";
                ap.message = pack.objName + " is connected.";
                ap.time = System.currentTimeMillis();

                if (AgentManager.getAgent(pack.objHash) != null) {
                    ap.objType = AgentManager.getAgent(pack.objHash).objType;
                } else {
                    ap.objType = "scouter";
                }

                alert(ap);
            } else if (op.alive == false) {
                // in case of agent reconnected
                ap = new AlertPack();
                ap.level = AlertLevel.INFO;
                ap.objHash = pack.objHash;
                ap.title = "An object has been activated.";
                ap.message = pack.objName + " is reconnected.";
                ap.time = System.currentTimeMillis();
                ap.objType = AgentManager.getAgent(pack.objHash).objType;

                alert(ap);
            }
            // inactive state can be handled in alert() method.
        }
    }

    @ServerPlugin(PluginConstants.PLUGIN_SERVER_XLOG)
    public void xlog(XLogPack pack) {
        if (conf.getBoolean("ext_plugin_exception_xlog_email_enabled", false )) {
            String serviceName = TextRD.getString(DateUtil.datetime(pack.endTime), TextTypes.SERVICE, pack.service);
            AlertPack ap = new AlertPack();
            if (pack.error != 0) {
                ap.level = AlertLevel.ERROR;
                ap.objHash = pack.objHash;
                ap.title = "xlog Error";
                ap.message = serviceName + " - " + TextRD.getString(DateUtil.datetime(pack.endTime), TextTypes.ERROR, pack.error);
                ap.time = System.currentTimeMillis();
                ap.objType = "scouter";
            }
            // Get agent Name
            String name = AgentManager.getAgentName(pack.objHash) == null ? "N/A" : AgentManager.getAgentName(pack.objHash);

            if ("/cjescwas01/escprd1".equals(name) || "/cjescwas02/escprd2".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_esc_email_enabled", false )){
                    alert(ap);
                }
            } else if("/cjwas03/expwas01".equals(name) || "/cjwas04/expwas02".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_exp_email_enabled", false )){
                    alert(ap);
                }
            } else if("/cjwas03/igap_was3".equals(name) || "/cjwas04/igap_was4".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_igap_email_enabled", false )){
                    alert(ap);
                }
            } else if("/cjwas03/tmsprd1-1".equals(name) || "/cjwas03/tmsprd1-2".equals(name) || "/cjwas04/tmsprd2-1".equals(name) || "/cjwas04/tmsprd2-2".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_tms_email_enabled", false )){
                    alert(ap);
                }
            } else if("/gprtwas1/wise_prd11".equals(name) || "/gprtwas1/wise_prd12".equals(name) || "/gprtwas2/wise_prd21".equals(name) || "/gprtwas2/wise_prd22".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_wise_email_enabled", false )) {
                    alert(ap);
                }
            } else if("/cjwas03/mproWas03".equals(name) || "/cjwas04/mproWas04".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_mpro_email_enabled", false )){
                    alert(ap);
                }
            } else if("/cjwas01/cis1".equals(name) || "/cjwas02/cis2".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_cis_email_enabled", false )){
                    alert(ap);
                }
            } else if("/cjodswas01/odsprd01".equals(name) || "/cjodswas02/odsprd02".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_ods_email_enabled", false )){
                    alert(ap);
                }
            } else if("/cjpcplwas1/cplwas1".equals(name) || "/cjpcplwas2/cplwas2".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_cpl_email_enabled", false )){
                    alert(ap);
                }
            } else if("/cjwas03/qmswas1".equals(name) || "/cjwas04/qmswas2".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_qms_email_enabled", false )){
                    alert(ap);
                }
            } else if("/cjirisap1/bmis_was1".equals(name) || "/cjemap/bmis_was2".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_bmis_email_enabled", false )){
                    alert(ap);
                }
            } else if("/cjirisap1/iris_was1".equals(name) || "/cjemap/iris_was2".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_iris_email_enabled", false )){
                    alert(ap);
                }
            } else if("/pEacA1/PFLS_LIVE1".equals(name) || "/pEacA2/PFLS_LIVE2".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_pfls_teams_enabled", false )){
                    alert(ap);
                }
            } else if("/cjwas03/amsprd_1".equals(name) || "/cjwas04/amsprd_1".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_ams_email_enabled", false )){
                    alert(ap);
                }
            } else if("/cjwas03/cmsprd_1".equals(name) || "/cjwas04/cmsprd_1".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_cms_email_enabled", false )){
                    alert(ap);
                }
            } else if("/CJHANAROWAS01/HANARO_PRD1".equals(name) || "/CJHANAROWAS02/HANARO_PRD2".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_hanaro_email_enabled", false )){
                    alert(ap);
                }
            } else if("/cj-meta-app/cj-meta-app".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_meta_email_enabled", false )){
                    alert(ap);
                }
            } else if("/CJFTAAP/fta".equals(name)) {
                if (conf.getBoolean("ext_plugin_exception_xlog_fta_email_enabled", false )){
                    alert(ap);
                }
            } else {
                if (conf.getBoolean("ext_plugin_exception_xlog_default_email_enabled", false )){
                    alert(ap);
                }
            }

            try {
                int elapsedThreshold = conf.getInt("ext_plugin_elapsed_time_threshold", 0);

                if (elapsedThreshold != 0 && pack.elapsed > elapsedThreshold) {
                    ap.level = AlertLevel.WARN;
                    ap.objHash = pack.objHash;
                    ap.title = "Elapsed time exceed a threshold.";
                    ap.message = "[" + AgentManager.getAgentName(pack.objHash) + "] "
                            + pack.service + "(" + serviceName + ") "
                            + "elapsed time(" + pack.elapsed + " ms) exceed a threshold.";
                    ap.time = System.currentTimeMillis();
                    ap.objType = AgentManager.getAgent(pack.objHash).objType;

                    alert(ap);
                }
            } catch (Exception e) {
                Logger.printStackTrace(e);
            }
        }
    }

    @ServerPlugin(PluginConstants.PLUGIN_SERVER_COUNTER)
    public void counter(PerfCounterPack pack) {
        String objName = pack.objName;
        int objHash = HashUtil.hash(objName);
        String objType = null;
        String objFamily = null;

        if (AgentManager.getAgent(objHash) != null) {
            objType = AgentManager.getAgent(objHash).objType;
        }

        if (objType != null) {
            objFamily = CounterManager.getInstance().getCounterEngine().getObjectType(objType).getFamily().getName();
        }

        try {
            // in case of objFamily is javaee
            if (CounterConstants.FAMILY_JAVAEE.equals(objFamily)) {
                // save javaee type's objHash
                if (!javaeeObjHashList.contains(objHash)) {
                    javaeeObjHashList.add(objHash);
                }

                if (pack.timetype == TimeTypeEnum.REALTIME) {
                    long gcTimeThreshold = conf.getLong("ext_plugin_gc_time_threshold", 0);
                    long gcTime = pack.data.getLong(CounterConstants.JAVA_GC_TIME);

                    long heapUsedThreshold = conf.getLong("ext_plugin_heap_used_threshold", 0);
                    long heapUsedThreshold_8G = conf.getLong("ext_plugin_8G_heap_used_threshold", 0);
                    long heapUsedThreshold_4G = conf.getLong("ext_plugin_4G_heap_used_threshold", 0);
                    long heapUsed = pack.data.getLong(CounterConstants.JAVA_HEAP_USED);

                    if("/gprtwas1/wise_prd11".equals(objName) || "/gprtwas1/wise_prd12".equals(objName) || "/gprtwas2/wise_prd21".equals(objName) || "/gprtwas2/wise_prd22".equals(objName)) {
                        if (heapUsedThreshold_8G != 0 && heapUsed > heapUsedThreshold_8G) {
                            AlertPack ap = new AlertPack();

                            ap.level = AlertLevel.FATAL;
                            ap.objHash = objHash;
                            ap.title = "Heap used exceed a threshold.";
                            ap.message = objName + " Heap uesd(" + heapUsed + " M) exceed a threshold.";
                            ap.time = System.currentTimeMillis();
                            ap.objType = objType;

                            alert(ap);
                        }
                    } else if("/cjwas03/expwas01".equals(objName) || "/cjwas04/expwas02".equals(objName) || "/cjwas03/qmswas1".equals(objName) || "/cjwas04/qmswas2".equals(objName) || "/cjwas03/amsprd_1".equals(objName) || "/cjwas04/amsprd_2".equals(objName) || "/cjwas03/cmsprd_1".equals(objName) || "/cjwas04/cmsprd_2".equals(objName) || "/cjirisap1/bmis_was1".equals(objName) || "/cjirisap1/iris_was1".equals(objName) || "/cjemap/bmis_was2".equals(objName) || "/cjemap/iris_was2".equals(objName)) {
                        if (heapUsedThreshold_4G != 0 && heapUsed > heapUsedThreshold_4G) {
                            AlertPack ap = new AlertPack();

                            ap.level = AlertLevel.FATAL;
                            ap.objHash = objHash;
                            ap.title = "Heap used exceed a threshold.";
                            ap.message = objName + " Heap uesd(" + heapUsed + " M) exceed a threshold.";
                            ap.time = System.currentTimeMillis();
                            ap.objType = objType;

                            alert(ap);
                        }
                    } else {
                        if (heapUsedThreshold != 0 && heapUsed > heapUsedThreshold) {
                            AlertPack ap = new AlertPack();

                            ap.level = AlertLevel.FATAL;
                            ap.objHash = objHash;
                            ap.title = "Heap used exceed a threshold.";
                            ap.message = objName + " Heap uesd(" + heapUsed + " M) exceed a threshold.";
                            ap.time = System.currentTimeMillis();
                            ap.objType = objType;

                            alert(ap);
                        }
                    }

                    if (gcTimeThreshold != 0 && gcTime > gcTimeThreshold) {
                        AlertPack ap = new AlertPack();

                        ap.level = AlertLevel.WARN;
                        ap.objHash = objHash;
                        ap.title = "GC time exceed a threshold.";
                        ap.message = objName + "'s GC time(" + gcTime + " ms) exceed a threshold.";
                        ap.time = System.currentTimeMillis();
                        ap.objType = objType;

                        alert(ap);
                    }
                }
            }
        } catch (Exception e) {
            Logger.printStackTrace(e);
        }
    }

    private void println(Object o) {
        if (conf.getBoolean("ext_plugin_email_debug", false)) {
            Logger.println(o);
        }
    }
}