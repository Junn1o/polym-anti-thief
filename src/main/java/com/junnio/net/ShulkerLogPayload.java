
package com.junnio.net;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ShulkerLogPayload(
        String playerName,
        String shulkerName,
        String position,
        String dimension,
        boolean isContainer,
        String actionName,
        String itemName
) implements CustomPayload {

    //public static final Identifier ID = Identifier.of("polymantithief", "shulker_log");
    public static final CustomPayload.Id<ShulkerLogPayload> ID =
            new CustomPayload.Id<>(Identifier.of("polymantithief", "shulker_log"));
    public static final Codec<ShulkerLogPayload> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("playerName").forGetter(ShulkerLogPayload::playerName),
                    Codec.STRING.fieldOf("shulkerName").forGetter(ShulkerLogPayload::shulkerName),
                    Codec.STRING.fieldOf("position").forGetter(ShulkerLogPayload::position),
                    Codec.STRING.fieldOf("dimension").forGetter(ShulkerLogPayload::dimension),
                    Codec.BOOL.fieldOf("isContainer").forGetter(ShulkerLogPayload::isContainer),
                    Codec.STRING.optionalFieldOf("actionName","").forGetter(ShulkerLogPayload::actionName),
                    Codec.STRING.optionalFieldOf("itemName","").forGetter(ShulkerLogPayload::itemName)
            ).apply(instance, ShulkerLogPayload::new)
    );

    public static final PacketCodec<RegistryByteBuf, ShulkerLogPayload> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ShulkerLogPayload::playerName,
            PacketCodecs.STRING, ShulkerLogPayload::shulkerName,
            PacketCodecs.STRING, ShulkerLogPayload::position,
            PacketCodecs.STRING, ShulkerLogPayload::dimension,
            PacketCodecs.BOOLEAN, ShulkerLogPayload::isContainer,
            PacketCodecs.STRING, ShulkerLogPayload::actionName,
            PacketCodecs.STRING, ShulkerLogPayload::itemName,
            ShulkerLogPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}