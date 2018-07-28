package net.floodlightcontroller.virtualrouter.store.gateway;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.virtualrouter.Gateway;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;

import java.util.Optional;
import java.util.Set;

public interface GatewayStoreService extends IFloodlightService {
    Optional<Gateway> getGateway(IPv4Address address, DatapathId switchId);

    boolean existGateway(DatapathId switchId);

    Set<Gateway> getGatewaysRegsiteredOnSwitch(DatapathId switchId);
}