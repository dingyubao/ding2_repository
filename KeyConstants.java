package org.onosproject.mongodb;

import java.util.concurrent.SynchronousQueue;

/**
 * Created by root on 11/30/17.
 */
public class KeyConstants {

    public static class sdwan {

        //DB of flexEdge

        /**
         * tunnelId + sn
         */
        public static final String ENG_IPSEC = "%d-%s";

        /**
         * 物理口名称 + sn
         */
        public static final String ENG_INTERFACEIP = "%s-%s";

        /**
         *静态路由业务id
         */
        public static final String ENG_STATICROUTE_RIP = "COMMON_STATIC_ROUTER-%s";

        /**
         *ipsec对端ip + 掩码 + vrfName + ipsec隧道名称
         */
        public static final String ENG_STATICROUTE_BGP_ADD = "bgpStaticRoute%s-%s-%s-%s";

        /**
         *ipsec对端ip + 掩码 + vrfName
         */
        public static final String ENG_STATICROUTE_BGP_DEL = "bgpStaticRoute%s-%s-%s";

        /**
         *tenantId + 地址池id + sn
         */
        public static final String ENG_IPPOOL = "ippool_group-%d-%s-%s";

        /**
         *NetworkIdentifier
         */
        public static final String ENG_IPPOOL_BYOD = "ippool_group-%s";

        /**
         * NetworkIdentifier
         */
        public static final String BYODNETWORK = "%s";

        /**
         *NetworkIdentifier + userName
         */
        public static final String BYODUSER = "%s-%s";

        /**
         *sn + vrfName
         */
        public static final String ENG_VRF = "%s-%s";

        /**
         *vlanSubInterfaceName + sn
         */
        public static final String ENG_SUBINTERFACE = "%s-%s";

        /**
         *InterfaceAlias + irbInterfaceName
         */
        public static final String ENG_IRBINTERFACE = "%s%s";

        /**
         * vrfName + neighborAddress
         */
        public static final String ENG_BGP = "%s_%s";

        /**
         * vrfName
         */
        public static final String ENG_BGPBASIC = "%s";

        /**
         *vrfName + ipNetSegment + vlanIds
         */
        public static final String ENG_BGPNETWORK = "%s_%s_%s";

        /**
         *"loopback" + vrfName + loopbackIp
         */
        public static final String ENG_BGPNETWORK_FROM_LOOPBACK = "%s_%s_%s";

        /**
         * interfaceName + sn
         */
        public static final String ENG_DHCP = "%s-%s";

        /**
         *aclName  (aclName == "acl_%d_%d" (tenantId + aclId))
         */
        public static final String ENG_ACLRULE = "%s";

        /**
         *qosStreamId + sn
         */
        public static final String ENG_QOS = "classmap%d-%s";

        /**
         * vfiName (vfiName有三种拼接方式)
         */
        public static final String ENG_VFI = "%s";

        /**
         * vlanInterfaceName
         */
        public static final String ENG_SERVICEINSTANCE = "%s";

        /**
         * qosStreamId + sn
         */
        public static final String ENG_QOSSTREAM = "qosStream-%s-%s";

        /**
         * tenantId + tunnelId
         */
        public static final String ENG_PINGDETECT = "%s-%s";

        /**
         * pointId + pointId + tunnelId
         */
        public static final String ENG_NQA= "nqa-%d-%d-%d";

        /**
         * 1024 or tenantId
         */
        public static final String ENG_LOOPBACK = "loopback%d";

        /**
         *vrfName + nextHopIp + tunnelId
         */
        public static final String ENG_TUNNELPOLICY= "tunnelPolicy-%s-%s-%d";

        /**
         *tenantId + tunnelId
         */
        public static final String ENG_BFDDETECT= "%s-%s";

//        public static final String ENGFLOW =  "engflow";

        /**
         *tenantId + aclId
         */
        public static final String ENG_FLOWSTRATEGY =  "acl_%d_%d";

        /**
         * tenantId + aclId
         */
        public static final String ENG_NAT =  "nat-%d-%d";

        /**
         * sn + tenantId + interfaceAlias + vlanIds
         */
        public static final String ENG_VLAN= "%s-%d-%s-%s";


        /**
         *ftpIp + ftpPort + ftpUser + ftpPassWord + ftpPath
         */
        public static final String ENG_UPGRADE= "%s-%d-%s-%s-%s";

        /**
         *sn
         */
        public static final String ENG_SYSINFO= "%s-system-mac";

        /**
         *tunnelId + sn
         */
        public static final String ENG_VXLAN="%d-%s";

        /**
         *tenantId + sn + vlanSubInterfaceName
         */
        public static final String ENG_OSPF="%d-%s-%s";

        /**
         *vrt + tunnelId
         */
        public static final String ENG_RIP="%s-%s";

        /**
         *sn
         */
        public static final String ENG_RIRBNUM= "%s";

        /**
         *tenantId
         */
        public static final String ENG_BGPVRF="vrf%s";

        /**
         *sn + staticNatId
         */
        public static final String ENG_STATICNAT="%s-%d";

        /**
         * vrfName + rt
         */
        public static final String ROUTETARGET = "%s-%s";






        //DB of flexThinEdge
        /**
         * sn
         */
        public static final String GW_LINK = "%s";

        //Used
        public static final String GW_MODE = "gwmode";

        /**
         * sn
         */
        public static final String GW_IPPOOL = "%s";

        //unUsed
        public static final String GW_ROUTE = "gwroute";

        /**
         * tunnelId + sn
         */
        public static final String GW_IPSEC = "%s-%s";

        //unUsed
        public static final String GW_VXLAN = "gwvxlan";

        /**
         * sn
         */
        public static final String GW_QOS = "%s";

        /**
         *ftpIp + ftpPort + ftpUser + ftpPassWord + ftpPath
         */
        public static final String GW_UPGRADE= "%s-%d-%s-%s-%s";
    }

    public static String getKey(String format, Object... param){
        return String.format(format, param);
    }
}
