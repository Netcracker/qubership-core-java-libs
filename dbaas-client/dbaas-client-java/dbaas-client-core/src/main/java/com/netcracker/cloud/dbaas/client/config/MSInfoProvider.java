package org.qubership.cloud.dbaas.client.config;


public interface MSInfoProvider {

    String getMicroserviceName();

    String getNamespace();

    String getLocalDevNamespace();

}
