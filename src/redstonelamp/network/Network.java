package redstonelamp.network;

import redstonelamp.Player;
import redstonelamp.RedstoneLamp;
import redstonelamp.Server;
import redstonelamp.network.packet.*;
import redstonelamp.utils.CompressionUtils;
import redstonelamp.utils.DynamicByteBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Networking class.
 */
public class Network {
    private Map<Byte, Class<? extends DataPacket>> packets = new ConcurrentHashMap<>();
    private Server server;
    private List<NetworkInterface> interfaces = new ArrayList<>();

    private Map<Player[], List<DataPacket>> toSend = new ConcurrentHashMap<>();

    public Network(Server server){
        this.server = server;
        registerPackets();
    }

    public void tick(){
        interfaces.forEach(redstonelamp.network.NetworkInterface::processData);

        for(Player[] players : toSend.keySet()){
            List<DataPacket> packets = toSend.get(players);
            for(Player player : players){
                packets.forEach(player::sendDirectDataPacket);
            }
            toSend.remove(players);
        }
    }

    public void addToSendQueue(Player[] i, DataPacket dp){
        if(toSend.containsKey(i)) {
            List<DataPacket> list = toSend.get(i);
            list.add(dp);
            toSend.put(i, list);
        } else {
            List<DataPacket> l = new CopyOnWriteArrayList<>();
            l.add(dp);
            toSend.put(i, l);
        }
    }

    public void processBatch(BatchPacket bp, Player player) {
        try{
            bp.payload = CompressionUtils.zlibInflate(bp.payload);
            int len = bp.payload.length;
            int offset = 0;
            while(offset < len){
                DataPacket pk = getPacket(bp.payload[offset++]);
                if(pk == null){
                    pk = new UnknownDataPacket();
                }
                pk.decode(bp.payload, offset);
                player.handleDataPacket(pk);
                offset =+ (pk.getOffset() - offset);
                if(offset >= bp.payload.length || offset < 0){
                    return;
                }
            }
        } catch(Exception e){
            server.getLogger().error("Exception: "+e.getMessage());
            if(server.isDebugMode()){
                server.getLogger().debug("Exception while handling BatchPacket 0x" + String.format("%02X ", bp.payload[0]));
                e.printStackTrace();
            }
        }
    }

    public void sendBatches(Player[] players, DataPacket[] packets, NetworkChannel channel){
        DynamicByteBuffer bb = DynamicByteBuffer.newInstance();
        for(DataPacket packet : packets){
            bb.put(packet.encode());
        }

        RedstoneLamp.getAsync().submit(() -> {
                    BatchPacket bp = new BatchPacket();
                    bp.setChannel(channel);
                    bp.payload = CompressionUtils.zlibDeflate(bb.toArray(), PENetworkInfo.COMPRESSION_LEVEL);
                    addToSendQueue(players, bp);
        }
        );
    }

    /**
     * Broadcasts a DataPacket to all players on this network.
     * @param packet The Packet to be sent.
     * @param playerClazz The type of player (PocketPlayer, DesktopPlayer etc)
     */
    public void broadcastPacket(DataPacket packet, Class<? extends Player> playerClazz){
        for(Player p : server.getOnlinePlayers()){
            if(p.getClass().getName().equals(playerClazz.getName())){
                p.sendDataPacket(packet);
            }
        }
    }

    public void setName(String name){
        for(NetworkInterface networkInterface : interfaces){
            networkInterface.setName(name);
        }
    }

    public void registerInterface(NetworkInterface interface_){
        interfaces.add(interface_);
    }

    public void removeInterface(NetworkInterface interface_){
        interfaces.remove(interface_);
    }

    public NetworkInterface getInterface(Class<? extends NetworkInterface> clazz){
        for(NetworkInterface networkInterface : interfaces){
            if(networkInterface.getClass().getName().equals(clazz.getName())){
                return networkInterface;
            }
        }
        return null;
    }

    public DataPacket getPacket(byte ID){
        if(packets.containsKey(ID)) {
            Class<? extends DataPacket> clazz = packets.get(ID);
            try {
                return clazz.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void registerPackets(){
        packets.put(AnimatePacket.ID, AnimatePacket.class);
        packets.put(BatchPacket.ID, BatchPacket.class);
        packets.put(ContainerSetContentPacket.ID, ContainerSetContentPacket.class);
        packets.put(DisconnectPacket.ID, DisconnectPacket.class);
        packets.put(LoginPacket.ID, LoginPacket.class);
        packets.put(PlayStatusPacket.ID, PlayStatusPacket.class);
        packets.put(SetDifficultyPacket.ID, SetDifficultyPacket.class);
        packets.put(SetEntityDataPacket.ID, SetEntityDataPacket.class);
        packets.put(SetEntityMotionPacket.ID, SetEntityMotionPacket.class);
        packets.put(SetHealthPacket.ID, SetHealthPacket.class);
        packets.put(SetSpawnPositionPacket.ID, SetSpawnPositionPacket.class);
        packets.put(SetTimePacket.ID, SetTimePacket.class);
        packets.put(StartGamePacket.ID, StartGamePacket.class);
        packets.put(TextPacket.ID, TextPacket.class);
        packets.put(MovePlayerPacket.ID, MovePlayerPacket.class);
        packets.put(RemoveBlockPacket.ID, RemoveBlockPacket.class);
    }

    public void shutdown() {
        interfaces.forEach(redstonelamp.network.NetworkInterface::shutdown);
    }

    public void emergencyShutdown() {
        interfaces.forEach(redstonelamp.network.NetworkInterface::emergencyShutdown);
    }
}
