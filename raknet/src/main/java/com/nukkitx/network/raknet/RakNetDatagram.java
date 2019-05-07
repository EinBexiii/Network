package com.nukkitx.network.raknet;

import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RakNetDatagram extends AbstractReferenceCounted {
    final List<EncapsulatedPacket> packets = new ArrayList<>();
    byte flags = (byte) 0x84;
    int sequenceIndex;

    @Override
    public RakNetDatagram retain() {
        super.retain();
        return this;
    }

    @Override
    public RakNetDatagram retain(int increment) {
        super.retain(increment);
        return this;
    }

    @Override
    public RakNetDatagram touch(Object hint) {
        for (EncapsulatedPacket packet : packets) {
            packet.touch(hint);
        }
        return this;
    }

    public void decode(ByteBuf buf) {
        flags = buf.readByte();
        sequenceIndex = buf.readUnsignedMediumLE();
        while (buf.isReadable()) {
            EncapsulatedPacket packet = new EncapsulatedPacket();
            packet.decode(buf);
            packets.add(packet);
        }
    }

    public void encode(ByteBuf buf) {
        buf.writeByte(flags);
        buf.writeMediumLE(sequenceIndex);
        for (EncapsulatedPacket packet : packets) {
            packet.encode(buf);
        }
    }

    boolean tryAddPacket(EncapsulatedPacket packet, int mtu) {
        int packetLn = packet.getSize();
        if (packetLn >= mtu - 4) {
            return false; // Packet is too large
        }

        int existingLn = 0;
        for (EncapsulatedPacket netPacket : this.packets) {
            existingLn += netPacket.getSize();
        }

        if (existingLn + packetLn >= mtu - 4) {
            return false;
        }

        packets.add(packet);
        if (packet.isSplit()) {
            flags |= RakNetConstants.FLAG_CONTINOUS_SEND;
        }
        return true;
    }

    @Override
    protected void deallocate() {
        for (EncapsulatedPacket packet : packets) {
            ReferenceCountUtil.release(packet);
        }
    }
}