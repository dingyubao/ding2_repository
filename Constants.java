package org.onosproject.mongodb;

/**
 * Created by root on 11/30/17.
 */
public class Constants {

    public static class sdwan {
        public static final String DBNAME="sdwan";
        //public static final String DBCANDIDATA="candidata";
        //public static final String DBRUNNING="running";

        //DB of common
        public static final String REGISTERDEVICE = "registerdevice";
        public static final String DEVICEDOMAIN = "devicedomain";
        public static final String NOTIFYTIMEMAP = "notifytimemap";
        public static final String MAGICBOX = "magicbox";
        public static final String FINGERPRINT = "fingerprint";

        public static final String RANDOMGENERATINGNUMBER = "RandomGeneratingNumber";
        public static final String LOOPBACKIPRESOURCE = "LoopbackIpResource";


        //DB of flexEdge
        public static final String ENGIPSEC="engipsec";

        public static final String ENGINTERFACEIP = "enginterfaceIp";

        public static final String ENGSTATICROUTE = "staticroute";

        public static final String ENGIPPOOL = "engippool";

        public static final String BYODNETWORK = "byodnetwork";

        public static final String BYODUSER= "byoduser";

        public static final String ENGVRF = "engvrf";

        public static final String ENGSUBINTERFACE = "engsubinterface";

        public static final String ENGIRBINTERFACE = "engirbinterface";

        public static final String ENGBGP = "engbgp";

        public static final String ENGBGPBASIC = "engbgpbasic";

        public static final String ENGBGPNETWORK = "engbgpnetwork";

        public static final String ENGDHCP = "engdhcp";

        public static final String ENGACLRULE = "engaclrule";

        public static final String ENGQOS = "engqos";

        public static final String ENGVFI = "engvfi";

        public static final String ENGSERVICEINSTANCE = "engserviceinstance";

        public static final String ENGQOSSTREAM = "engqosstream";

        public static final String ENGPINGDETECT = "engpingdetect";

        public static final String ENGNQA= "engnqa";

        public static final String ENGLOOPBACK = "engbgploopback";

        public static final String ENGTUNNELPOLICY= "engtunnelpolicy";

        public static final String ENGBFDDETECT= "engbfddetect";

//        public static final String ENGFLOW =  "engflow";

        public static final String ENGFLOWSTRATEGY =  "engflowstrategy";

        public static final String ENGNAT =  "engnat";

        public static final String ENGVLAN= "engvlan";


        public static final String ENGUPGRADE= "engupgrade";

        public static final String ENGSYSINFO= "engsysteminfo";

        public static final String ENGVXLAN="engvxlan";

        public static final String ENGOSPF="engospf";

        public static final String ENGRIP="engrip";

        public static final String ENGRIRBNUM="engirbnumber";

        public static final String ENGBGPVRF="engbgpvrf";

        public static final String ENGSTATICNAT="engstaticnat";

        //DB of flexThinEdge
        public static final String GWLINK = "gwlink";

        public static final String GWMODE = "gwmode";

        public static final String GWIPPOOL = "gwippool";

        public static final String GWROUTE = "gwroute";

        public static final String GWIPSEC = "gwipsec";

        public static final String GWVXLAN = "gwvxlan";

        public static final String ROUTETARGET = "routetarget";

        public static final String GWQOS = "gwqos";

        public static final String GWUPGRADE= "gwupgrade";
    }

    public enum DBType {
        CANDIDATE("Candidate"),
        RUNNING("Running"),
        SHARE("Share");

        private String value;

        DBType(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static DBType fromValue(String text) {
            for (DBType b : DBType.values()) {
                if (String.valueOf(b.value).equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    public enum DevModel {
        ENG("FlexEdge"),
        GW("FlexThinEdge"),
        CONTROLLER("Controller");

        private String value;

        DevModel(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static DevModel fromValue(String text) {
            for (DevModel b : DevModel.values()) {
                if (String.valueOf(b.value).equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    /*
     * NOTICE: BusinessType的枚举值和它的字符串描述（例如 IPSEC("Ipsec")），可以不区分小大写，但是必须相等
     * NOTICE: 建议名称尽量控制在10个字符之内
     */
    public enum BusinessType {
        IPSEC("Ipsec"),
        PINGDETECT("PingDetect"),
        BFD("BFD"),
        LINK("Link"),
        BGPBASIC("BgpBasic"),
        BGP("Bgp"),
        BGPNETWORK("BgpNetwork"),
        TUNNELPOLICY("TunnelPolicy"),
        BYODNETWORK("ByodNetwork"),
        BYODUSER("ByodUser"),
        IPPOOL("IpPool"),
        MODE("Mode"),
        TENANT("Tenant"),
        QOS("Qos"),
        UPGRADE("Upgrade"),
        PHYSICALINTERFACE("PhysicalInterface"),
        VLAN("Vlan"),
        STATICROUTE("StaticRoute"),
        SYSTEMINFO("SystemInfo"),
        QOSSTREAM("QosStream"),
        VXLAN("Vxlan"),
        DEVICEDOMAIN("DeviceDomain"),
        FLOW("Flow"),
        FLOWSTRATEGY("FlowStrategy"),
        NAT("Nat"),
        NQA("Nqa"),
        OSPF("Ospf"),
        RIP("Rip"),
        STATICNAT("StaticNat");


        private String value;

        BusinessType(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static BusinessType fromValue(String text) {
            for (BusinessType b : BusinessType.values()) {
                if (String.valueOf(b.value).equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    public static String candidateDBName(String sn, DevModel model) {
        if (sn == null || sn.isEmpty() || model == null) {
            return null;
        }

        String fmt = new String(DBType.CANDIDATE.toString());
        return fmt.concat("-").concat(model.toString()).concat("-").concat(sn);
    }

    public static String runningDBName(String sn, DevModel model) {
        if (sn == null || sn.isEmpty() || model == null) {
            return null;
        }

        String fmt = new String(DBType.RUNNING.toString());
        return fmt.concat("-").concat(model.toString()).concat("-").concat(sn);
    }
}
