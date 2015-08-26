/**
 * This file is part of RedstoneLamp.
 * <p>
 * RedstoneLamp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * RedstoneLamp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with RedstoneLamp.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.redstonelamp.network.pe.sub.v27;

import net.redstonelamp.Player;
import net.redstonelamp.level.position.Position;
import net.redstonelamp.network.UniversalPacket;
import net.redstonelamp.network.pe.sub.PESubprotocolManager;
import net.redstonelamp.network.pe.sub.Subprotocol;
import net.redstonelamp.nio.BinaryBuffer;
import net.redstonelamp.request.ChatRequest;
import net.redstonelamp.request.LoginRequest;
import net.redstonelamp.request.PlayerMoveRequest;
import net.redstonelamp.request.Request;
import net.redstonelamp.response.*;
import net.redstonelamp.utils.CompressionUtils;

import java.net.SocketAddress;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.DataFormatException;

/**
 * A subprotocol implementation for the MCPE version 0.11.1 (protocol 27)
 *
 * @author RedstoneLamp Team
 */
public class SubprotocolV27 extends Subprotocol implements ProtocolConst27{

    public SubprotocolV27(PESubprotocolManager manager){
        super(manager);
    }

    @Override
    public Request[] handlePacket(UniversalPacket up){
        List<Request> requests = new ArrayList<>();
        byte id = up.bb().getByte();
        if(id == BATCH_PACKET){
            return processBatch(up);
        }

        switch(id){
            case LOGIN_PACKET:
                getProtocol().getServer().getLogger().debug("Got Login packet!");
                LoginRequest lr = new LoginRequest(up.bb().getString());
                up.bb().skip(8); //Skip protocol1, protocol 2 (int, int)
                lr.clientId = up.bb().getInt();
                lr.slim = up.bb().getByte() > 0;
                //lr.skin = up.bb().get(up.bb().getUnsignedShort()); //Skin written as a String, but the String class seems to corrupt the Skin
                up.bb().skip(2);
                lr.skin = up.bb().get(up.bb().remaining());

                requests.add(lr);
                break;
            case TEXT_PACKET:
                ChatRequest cr = new ChatRequest(up.bb().getByte());
                switch(cr.type){
                    case TEXT_CHAT:
                        cr.source = up.bb().getString();
                    case TEXT_RAW:
                    case TEXT_POPUP:
                    case TEXT_TIP:
                        cr.message = up.bb().getString();
                        break;
                    case TEXT_TRANSLATION:
                        cr.message = up.bb().getString();
                        for(int i = 0; i < up.bb().getByte(); i++)
                            cr.parameters[i] = up.bb().getString();
                        break;
                }
                // TODO: Throw PlayerChatEvent
                getProtocol().getServer().getPlayer(up.getAddress()).sendPopup("You sent a message!");
                requests.add(cr);
                break;
            case MOVE_PLAYER_PACKET:
                Position position = new Position(getProtocol().getServer().getPlayer(up.getAddress()).getPosition().getLevel());
                up.bb().skip(8); //Skip entity ID
                float x = up.bb().getFloat();
                float y = up.bb().getFloat();
                float z = up.bb().getFloat();
                float yaw = up.bb().getFloat();
                up.bb().skip(4); //Skip bodyYaw
                float pitch = up.bb().getFloat();
                up.bb().skip(1); //Skip mode
                boolean onGround = up.bb().getByte() > 0;
                position.setX(x);
                position.setY(y);
                position.setZ(z);
                position.setYaw(yaw);
                position.setPitch(pitch);

                PlayerMoveRequest pmr = new PlayerMoveRequest(position, onGround);
                requests.add(pmr);
                break;
        }
        return requests.toArray(new Request[requests.size()]);
    }

    @Override
    public UniversalPacket[] translateResponse(Response response, Player player){
        SocketAddress address = player.getAddress();
        List<UniversalPacket> packets = new CopyOnWriteArrayList<>();
        BinaryBuffer bb;
        if(response instanceof LoginResponse){
            LoginResponse lr = (LoginResponse) response;
            if(lr.loginAllowed){
                bb = BinaryBuffer.newInstance(5, ByteOrder.BIG_ENDIAN);
                bb.putByte(PLAY_STATUS_PACKET);
                bb.putInt(0); //LOGIN_SUCCESS
                packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));

                bb = BinaryBuffer.newInstance(48, ByteOrder.BIG_ENDIAN);
                bb.putByte(START_GAME_PACKET);
                bb.putInt(-1); //seed
                bb.putInt(lr.generator);
                bb.putInt(lr.gamemode);
                bb.putLong(lr.entityID);
                bb.putInt(lr.spawnX);
                bb.putInt(lr.spawnY);
                bb.putInt(lr.spawnZ);
                bb.putFloat(lr.x);
                bb.putFloat(lr.y);
                bb.putFloat(lr.z);
                packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));

                bb = BinaryBuffer.newInstance(6, ByteOrder.BIG_ENDIAN);
                bb.putByte(SET_TIME_PACKET);
                bb.putInt(0); //TODO: Correct time
                bb.putByte((byte) 1);
                packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));

                bb = BinaryBuffer.newInstance(10, ByteOrder.BIG_ENDIAN);
                bb.putByte(SET_SPAWN_POSITION_PACKET);
                bb.putInt(lr.spawnX);
                bb.putInt(lr.spawnZ);
                bb.putByte((byte) lr.spawnY);
                packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));

                bb = BinaryBuffer.newInstance(5, ByteOrder.BIG_ENDIAN);
                bb.putByte(SET_HEALTH_PACKET);
                bb.putInt(20); //TODO: Correct health
                packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));

                bb = BinaryBuffer.newInstance(5, ByteOrder.BIG_ENDIAN);
                bb.putByte(SET_DIFFICULTY_PACKET);
                bb.putInt(1); //TODO: Correct difficulty
                packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));

                //TODO: If creative, send items

                getProtocol().getChunkSender().registerChunkRequests(getProtocol().getServer().getPlayer(address), 96);
            }else{
                String message;
                switch(lr.loginNotAllowedReason){
                    case LoginResponse.DEFAULT_loginNotAllowedReason:
                        message = "disconnectionScreen.noReason";
                        break;

                    case "redstonelamp.loginFailed.serverFull":
                        message = "disconnectionScreen.serverFull";
                        break;

                    default:
                        message = lr.loginNotAllowedReason;
                }

                bb = BinaryBuffer.newInstance(3 + message.getBytes().length, ByteOrder.BIG_ENDIAN);
                bb.putByte(DISCONNECT_PACKET);
                bb.putString(message);

                packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));
            }
        }else if(response instanceof DisconnectResponse){
            DisconnectResponse dr = (DisconnectResponse) response;
            if(dr.notifyClient){
                bb = BinaryBuffer.newInstance(3 + dr.reason.getBytes().length, ByteOrder.BIG_ENDIAN);
                bb.putByte(DISCONNECT_PACKET);
                bb.putString(dr.reason);

                packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));
            }
        }else if(response instanceof ChunkResponse){
            ChunkResponse cr = (ChunkResponse) response;

            BinaryBuffer ordered = BinaryBuffer.newInstance(83200, ByteOrder.BIG_ENDIAN);
            ordered.put(cr.chunk.getBlockIds());
            ordered.put(cr.chunk.getBlockMeta());
            ordered.put(cr.chunk.getSkylight());
            ordered.put(cr.chunk.getBlocklight());
            ordered.put(cr.chunk.getHeightmap());
            ordered.put(cr.chunk.getBiomeColors());

            byte[] orderedData = ordered.toArray();
            ordered = null;

            bb = BinaryBuffer.newInstance(83213, ByteOrder.BIG_ENDIAN);
            bb.putByte(FULL_CHUNK_DATA_PACKET);
            bb.putInt(cr.chunk.getPosition().getX());
            bb.putInt(cr.chunk.getPosition().getZ());
            bb.putInt(orderedData.length);
            bb.put(orderedData);

            packets.add(new UniversalPacket(Arrays.copyOf(bb.toArray(), bb.getPosition()), ByteOrder.BIG_ENDIAN, address));
        }else if(response instanceof SpawnResponse){
            SpawnResponse sr = (SpawnResponse) response;

            int flags = 0;
            flags |= 0x20;
            if(player.getGamemode() == 1){
                flags |= 0x80; //allow flight
            }
            bb = BinaryBuffer.newInstance(5, ByteOrder.BIG_ENDIAN);
            bb.putByte(ADVENTURE_SETTINGS_PACKET);
            bb.putInt(flags);
            packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));

            /*
            byte[] metadata = player.getMetadata().toBytes();
            bb = BinaryBuffer.newInstance(9 + metadata.length, ByteOrder.BIG_ENDIAN);
            bb.putByte(SET_ENTITY_DATA_PACKET);
            bb.putLong(0); //Player Entity ID is always zero to themselves
            bb.put(metadata);
            packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));
            */

            bb = BinaryBuffer.newInstance(6, ByteOrder.BIG_ENDIAN);
            bb.putByte(SET_TIME_PACKET);
            bb.putInt(player.getPosition().getLevel().getTime());
            bb.putByte((byte) 1);
            packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));

            bb = BinaryBuffer.newInstance(13, ByteOrder.BIG_ENDIAN);
            bb.putByte(RESPAWN_PACKET);
            bb.putFloat((float) player.getPosition().getX());
            bb.putFloat((float) player.getPosition().getY());
            bb.putFloat((float) player.getPosition().getZ());
            packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));

            bb = BinaryBuffer.newInstance(5, ByteOrder.BIG_ENDIAN);
            bb.putByte(PLAY_STATUS_PACKET);
            bb.putInt(3); //PLAY_SPAWN
            packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));
        }else if(response instanceof TeleportResponse){
            TeleportResponse tr = (TeleportResponse) response;
            bb = BinaryBuffer.newInstance(36, ByteOrder.BIG_ENDIAN);
            bb.putByte(MOVE_PLAYER_PACKET);
            bb.putLong(player.getEntityID());
            bb.putFloat((float) tr.pos.getX());
            bb.putFloat((float) tr.pos.getY());
            bb.putFloat((float) tr.pos.getZ());
            bb.putFloat(tr.pos.getYaw());
            bb.putFloat(tr.bodyYaw);
            bb.putFloat(tr.pos.getPitch());
            bb.putByte((byte) 0); //MODE_NORMAL
            bb.putByte((byte) (tr.onGround ? 1 : 0));
            packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));
        }else if(response instanceof ChatResponse){
            ChatResponse cr = (ChatResponse) response;
            bb = BinaryBuffer.newInstance(0, ByteOrder.BIG_ENDIAN); //Self-expand
            bb.putByte(TEXT_PACKET);
            if(cr.source != ""){
                bb.putByte(TEXT_CHAT); //TYPE_CHAT
                bb.putString(cr.source);
                bb.putString(cr.message);
            }else{
                bb.putByte(TEXT_RAW); //TYPE_RAW
                bb.putString(cr.message);
            }
            packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));
        }else if(response instanceof AddPlayerResponse){
            Player p = ((AddPlayerResponse) response).player;
            byte[] meta = p.getMetadata().toBytes();
            bb = BinaryBuffer.newInstance(56 + p.getSkin().length + p.getNametag().getBytes().length + meta.length, ByteOrder.BIG_ENDIAN);
            //bb = BinaryBuffer.newInstance(0, ByteOrder.BIG_ENDIAN);
            bb.putByte(ADD_PLAYER_PACKET);
            bb.putLong(p.getEntityID()); //Prevent client from knowing the real clientID
            bb.putString(p.getNametag());
            bb.putLong(p.getEntityID());
            bb.putFloat((float) p.getPosition().getX());
            bb.putFloat((float) p.getPosition().getY());
            bb.putFloat((float) p.getPosition().getZ());
            bb.putFloat(0f); //Speed X
            bb.putFloat(0f); //Speed y
            bb.putFloat(0f); //speed z
            bb.putFloat(p.getPosition().getYaw());
            bb.putFloat(p.getPosition().getYaw()); //TODO: head yaw
            bb.putFloat(p.getPosition().getPitch());
            bb.putShort((short) 0);
            bb.putShort((short) 0);
            bb.putByte((byte) (p.isSlim() ? 1 : 0));
            bb.putShort((short) p.getSkin().length);
            bb.put(p.getSkin());
            bb.put(meta);
            packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));
        }else if(response instanceof PopupResponse){
            PopupResponse pr = (PopupResponse) response;
            bb = BinaryBuffer.newInstance(0, ByteOrder.BIG_ENDIAN);
            bb.putByte(TEXT_PACKET);
            bb.putByte(TEXT_POPUP); // TYPE_POPUP
            bb.putString(pr.message);
            packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));
        }else if(response instanceof RemovePlayerResponse){
            RemovePlayerResponse rpp = (RemovePlayerResponse) response;
            bb = BinaryBuffer.newInstance(9, ByteOrder.BIG_ENDIAN);
            bb.putByte(REMOVE_PLAYER_PACKET);
            bb.putLong(rpp.player.getEntityID());
            bb.putLong(rpp.player.getEntityID());
            packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));
        }else if(response instanceof PlayerMoveResponse){
            PlayerMoveResponse pmr = (PlayerMoveResponse) response;
            bb = BinaryBuffer.newInstance(36, ByteOrder.BIG_ENDIAN);
            bb.putByte(MOVE_PLAYER_PACKET);
            bb.putLong(player.getEntityID());
            bb.putFloat((float) pmr.pos.getX());
            bb.putFloat((float) pmr.pos.getY());
            bb.putFloat((float) pmr.pos.getZ());
            bb.putFloat(pmr.pos.getYaw());
            bb.putFloat(pmr.bodyYaw);
            bb.putFloat(pmr.pos.getPitch());
            bb.putByte((byte) 0); //MODE_NORMAL
            bb.putByte((byte) (pmr.onGround ? 1 : 0));
            packets.add(new UniversalPacket(bb.toArray(), ByteOrder.BIG_ENDIAN, address));
        }

        //Compress packets
        packets.stream().filter(packet -> packet.getBuffer().length >= 512 && packet.getBuffer()[0] != BATCH_PACKET).forEach(packet -> { //Compress packets
            byte[] compressed = CompressionUtils.zlibDeflate(packet.getBuffer(), 7);
            BinaryBuffer batch = BinaryBuffer.newInstance(5 + compressed.length, ByteOrder.BIG_ENDIAN);
            batch.putByte(BATCH_PACKET);
            batch.putInt(compressed.length);
            batch.put(compressed);

            packets.remove(packet);
            packets.add(new UniversalPacket(batch.toArray(), ByteOrder.BIG_ENDIAN, address));
        });
        return packets.toArray(new UniversalPacket[packets.size()]);
    }


    private Request[] processBatch(UniversalPacket up){
        List<Request> requests = new ArrayList<>();
        int len = up.bb().getInt();
        byte[] compressed = up.bb().get(len);
        try{
            byte[] uncompressed = CompressionUtils.zlibInflate(compressed);
            BinaryBuffer bb = BinaryBuffer.wrapBytes(uncompressed, up.bb().getOrder());
            while(bb.getPosition() < uncompressed.length){
                byte id = bb.getByte();
                if(id == BATCH_PACKET){
                    throw new IllegalStateException("BatchPacket found inside BatchPacket!");
                }
                int start = bb.getPosition();
                UniversalPacket pk = new UniversalPacket(uncompressed, up.bb().getOrder(), up.getAddress());
                pk.bb().setPosition(start - 1); //subtract by one so the handler can read the packet ID
                requests.add(handlePacket(pk)[0]);
                bb.setPosition(pk.bb().getPosition());
            }
        }catch(DataFormatException e){
            getProtocol().getManager().getServer().getLogger().warning(e.getClass().getName() + " while handling BatchPacket: " + e.getMessage());
            getProtocol().getManager().getServer().getLogger().trace(e);
        }finally{
            if(!requests.isEmpty()){
                return requests.toArray(new Request[requests.size()]);
            }
            return new Request[]{null};
        }
    }

    @Override
    public String getMCPEVersion(){
        return MCPE_VERSION;
    }

    @Override
    public int getProtocolVersion(){
        return MCPE_PROTOCOL;
    }
}
