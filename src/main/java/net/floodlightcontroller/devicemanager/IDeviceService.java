/**
*    Copyright 2011,2012, Big Switch Networks, Inc. 
*    Originally created by David Erickson, Stanford University
* 
*    Licensed under the Apache License, Version 2.0 (the "License"); you may
*    not use this file except in compliance with the License. You may obtain
*    a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*    License for the specific language governing permissions and limitations
*    under the License.
**/

package net.floodlightcontroller.devicemanager;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nonnull;

import net.floodlightcontroller.devicemanager.internal.Device;
import net.floodlightcontroller.devicemanager.internal.Entity;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.VlanVid;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.FloodlightContextStore;
import net.floodlightcontroller.core.module.IFloodlightService;

/**
 * Device manager allows interacting with devices on the network.  Note
 * that under normal circumstances, {@link Device} objects should be retrieved
 * from the {@link FloodlightContext} rather than from {@link IDeviceManager}.
 */
public interface IDeviceService extends IFloodlightService {

    Device registerDevice(Entity entity);

    /**
     * Fields used in devices for indexes and querying
     * @see IDeviceService#addIndex
     */
    enum DeviceField {
        MAC, IPv4, IPv6, VLAN, SWITCH, PORT
    }

    /**
     * The source device for the current packet-in, if applicable.
     */
    public static final String CONTEXT_SRC_DEVICE = 
            "net.floodlightcontroller.devicemanager.srcDevice"; 

    /**
     * The destination device for the current packet-in, if applicable.
     */
    public static final String CONTEXT_DST_DEVICE = 
            "net.floodlightcontroller.devicemanager.dstDevice"; 

    /**
     * The original destination device for the current packet-in
     */
    public static final String CONTEXT_ORIG_DST_DEVICE =
            "net.floodlightcontroller.devicemanager.origDstDevice";

    /**
     * A FloodlightContextStore object that can be used to interact with the 
     * FloodlightContext information created by BVS manager.
     */
    public static final FloodlightContextStore<IDevice> fcStore = 
        new FloodlightContextStore<IDevice>();

    /**
     * Get the device with the given device key.
     * 
     * @param deviceKey the key to search for
     * @return the device associated with the key, or null if no such device
     * @see IDevice#getDeviceKey()
     */
    public IDevice getDevice(Long deviceKey);
    
    /**
     * Search for a device exactly matching the provided device fields. This 
     * is the same lookup process that is used for packet_in processing and 
     * device learning. Thus, findDevice() can be used to match flow entries
     * from switches to devices. 
     * 
     * Only the key fields as defined by the {@link IEntityClassifierService} will
     * be important in this search. All key fields MUST be supplied.
     * 
     *{@link queryDevices()} might be more appropriate!
     * 
     * @param macAddress the MAC address; use MacAddress.NONE for 'don't care'
     * @param vlan the VLAN; use VlanVid.ZERO for 'untagged'
     * @param ipv4Address the ipv4 address; use IPv4Address.NONE for 'don't care'
     * @param ipv6Address the ipv6 address; use IPv6Address.NONE for 'don't care'
     * @param switchDPID the switch DPID; use DatapathId.NONE for 'don't care'
     * @param switchPort the switch port; use OFPort.ZERO for 'don't care'
     * @return an {@link IDevice} or null if no device is found.
     * @see IDeviceManager#setEntityClassifier(IEntityClassifierService)
     * @throws IllegalArgumentException if not all key fields of the
     * current {@link IEntityClassifierService} are specified.
     */
    public IDevice findDevice(@Nonnull MacAddress macAddress, VlanVid vlan,
                              @Nonnull IPv4Address ipv4Address, @Nonnull IPv6Address ipv6Address,
                              @Nonnull DatapathId switchDPID, @Nonnull OFPort switchPort)
                              throws IllegalArgumentException;
    
    /**
     * Get a destination device using entity fields that corresponds with
     * the given source device.  The source device is important since
     * there could be ambiguity in the destination device without the
     * attachment point information. 
     * Search for a device in a given entity class. This is the same as 
     * the lookup process for destination devices. 
     * 
     * Only the key fields as defined by the reference entity class will
     * be important in this search. All key fields MUST be supplied.
     * 
     * @param entityClass The entity class in which to perform the lookup.
     * @param macAddress the MAC address; use MacAddress.NONE for 'don't care'
     * @param vlan the VLAN; use VlanVid.ZERO for 'untagged'
     * @param ipv4Address the ipv4 address; use IPv4Address.NONE for 'don't care'
     * @param ipv6Address the ipv6 address; use IPv6Address.NONE for 'don't care'
     * @return an {@link IDevice} or null if no device is found.
     * @see IDeviceService#findDevice(MacAddress, VlanVid, IPv4Address, IPv6Address, DatapathId, OFPort)
     * @throws IllegalArgumentException if not all key fields of the
     * source's {@link IEntityClass} are specified.
     */
    public IDevice findClassDevice(@Nonnull IEntityClass entityClass,
                                   @Nonnull MacAddress macAddress, VlanVid vlan,
                                   @Nonnull IPv4Address ipv4Address, @Nonnull IPv6Address ipv6Address)
                                   throws IllegalArgumentException;

    /**
     * Get an unmodifiable collection view over all devices currently known.
     * @return the collection of all devices
     */
    public Collection<? extends IDevice> getAllDevices();

    /**
     * Create an index over a set of fields.  This allows efficient lookup
     * of devices when querying using the indexed set of specified fields.
     * The index must be registered before any device learning takes place,
     * or it may be incomplete.  It's OK if this is called multiple times with
     * the same fields; only one index will be created for each unique set of 
     * fields.
     * 
     * @param perClass set to true if the index should be maintained for each
     * entity class separately.
     * @param keyFields the set of fields on which to index
     */
    public void addIndex(boolean perClass,
                         EnumSet<DeviceField> keyFields);
    
    /**
     * Find devices that match the provided query.  Any fields that are
     * null will not be included in the query.  If there is an index for 
     * the query, then it will be performed efficiently using the index.
     * Otherwise, there will be a full scan of the device list.
     * 
     * @param macAddress the MAC address; use MacAddress.NONE for 'don't care'
     * @param vlan the VLAN; use null for 'don't care'; use VlanVid.ZERO for 'untagged'
     * @param ipv4Address the ipv4 address; use IPv4Address.NONE for 'don't care'
     * @param ipv6Address the ipv6 address; use IPv6Address.NONE for 'don't care'
     * @param switchDPID the switch DPID; use DatapathId.NONE for 'don't care'
     * @param switchPort the switch port; use OFPort.ZERO for 'don't care'
     * @return an iterator over a set of devices matching the query
     * @see IDeviceService#queryClassDevices(IEntityClass, MacAddress, 
     * VlanVid, IPv4Address, IPv6Address, DatapathId, OFPort)
     */
    public Iterator<? extends IDevice> queryDevices(@Nonnull MacAddress macAddress,
                                                    VlanVid vlan,
                                                    @Nonnull IPv4Address ipv4Address, 
                                                    @Nonnull IPv6Address ipv6Address,
                                                    @Nonnull DatapathId switchDPID,
                                                    @Nonnull OFPort switchPort);

    /**
     * Find devices that match the provided query.  Only the index for
     * the specified class will be searched.  
     * Any fields that are null will not be included in the query.  If
     * there is an index for the query, then it will be performed
     * efficiently using the index. Otherwise, there will be a full scan
     * of the device list.
     * 
     * @param entityClass The entity class in which to perform the query
     * @param macAddress the MAC address; use MacAddress.NONE for 'don't care'
     * @param vlan the VLAN; use null for 'don't care'; use VlanVid.ZERO for 'untagged'
     * @param ipv4Address the ipv4 address; use IPv4Address.NONE for 'don't care'
     * @param ipv6Address the ipv6 address; use IPv6Address.NONE for 'don't care'
     * @param switchDPID the switch DPID; use DatapathId.NONE for 'don't care'
     * @param switchPort the switch port; use OFPort.ZERO for 'don't care'
     * @return an iterator over a set of devices matching the query
     * @see IDeviceService#queryClassDevices(MacAddress, 
     * VlanVid, IPv4Address, IPv6Address, DatapathId, OFPort)
     */
    public Iterator<? extends IDevice> queryClassDevices(IEntityClass entityClass,
                                                         MacAddress macAddress,
                                                         VlanVid vlan,
                                                         IPv4Address ipv4Address, 
                                                         IPv6Address ipv6Address,
                                                         DatapathId switchDPID,
                                                         OFPort switchPort);
    
    /**
     * Adds a listener to listen for IDeviceManagerServices notifications
     * 
     * @param listener The listener that wants the notifications
     * @param type     The type of the listener
     */
    public void addListener(IDeviceListener listener);
    
    /**
     * Specify points in the network where attachment points are not to
     * be learned.
     * @param sw
     * @param port
     */
    public void addSuppressAPs(DatapathId swId, OFPort port);

    public void removeSuppressAPs(DatapathId swId, OFPort port);

    public Set<SwitchPort> getSuppressAPs();

}
