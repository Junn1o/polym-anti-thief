
package com.junnio.net;

import com.junnio.Polymantithief;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ShulkerLogPayload(
        String playerName,
        String shulkerName,
        Float x,
        Float y,
        Float z,
        String dimension,
        boolean isContainer,
        String actionName,
        String itemName
) implements CustomPayload {

    //public static final Identifier ID = Identifier.of("polymantithief", "shulker_log");
    public static final CustomPayload.Id<ShulkerLogPayload> ID =
            new CustomPayload.Id<>(Identifier.of(Polymantithief.MOD_ID, "shulker_log"));
//    public static final Codec<ShulkerLogPayload> CODEC = RecordCodecBuilder.create(instance ->
//            instance.group(
//                    Codec.STRING.fieldOf("playerName").forGetter(ShulkerLogPayload::playerName),
//                    Codec.STRING.fieldOf("shulkerName").forGetter(ShulkerLogPayload::shulkerName),
//                    Codec.FLOAT.fieldOf("x").forGetter(ShulkerLogPayload::x),
//                    Codec.FLOAT.fieldOf("y").forGetter(ShulkerLogPayload::y),
//                    Codec.FLOAT.fieldOf("z").forGetter(ShulkerLogPayload::z),
//                    Codec.STRING.fieldOf("dimension").forGetter(ShulkerLogPayload::dimension),
//                    Codec.BOOL.fieldOf("isContainer").forGetter(ShulkerLogPayload::isContainer),
//                    Codec.STRING.optionalFieldOf("actionName","").forGetter(ShulkerLogPayload::actionName),
//                    Codec.STRING.optionalFieldOf("itemName","").forGetter(ShulkerLogPayload::itemName)
//            ).apply(instance, ShulkerLogPayload::new)
//    );

//    public static final PacketCodec<RegistryByteBuf, ShulkerLogPayload> PACKET_CODEC = PacketCodec.tuple(
//            PacketCodecs.STRING, ShulkerLogPayload::playerName,
//            PacketCodecs.STRING, ShulkerLogPayload::shulkerName,
//            PacketCodecs.FLOAT, ShulkerLogPayload::x,
//            PacketCodecs.FLOAT, ShulkerLogPayload::y,
//            PacketCodecs.FLOAT, ShulkerLogPayload::z,
//            PacketCodecs.STRING, ShulkerLogPayload::dimension,
//            PacketCodecs.BOOLEAN, ShulkerLogPayload::isContainer,
//            PacketCodecs.STRING, ShulkerLogPayload::actionName,
//            PacketCodecs.STRING, ShulkerLogPayload::itemName,
//            ShulkerLogPayload::new
//    );

    public static final PacketCodec<RegistryByteBuf, ShulkerLogPayload> PACKET_CODEC = new PacketCodec<>() {
        @Override
        public ShulkerLogPayload decode(RegistryByteBuf buf) {
            return new ShulkerLogPayload(
                    buf.readString(),
                    buf.readString(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readString(),
                    buf.readBoolean(),
                    buf.readString(),
                    buf.readString()
            );
        }

        @Override
        public void encode(RegistryByteBuf buf, ShulkerLogPayload payload) {
            buf.writeString(payload.playerName());
            buf.writeString(payload.shulkerName());
            buf.writeFloat(payload.x());
            buf.writeFloat(payload.y());
            buf.writeFloat(payload.z());
            buf.writeString(payload.dimension());
            buf.writeBoolean(payload.isContainer());
            buf.writeString(payload.actionName());
            buf.writeString(payload.itemName());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}