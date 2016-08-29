//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package fuud.copied;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass.RequestEnvelope.Builder;
import POGOProtos.Networking.Envelopes.SignatureOuterClass.Signature.DeviceInfo;
import POGOProtos.Networking.Envelopes.Unknown6OuterClass.Unknown6;
import POGOProtos.Networking.Envelopes.Unknown6OuterClass.Unknown6.Unknown2;
import POGOProtos.Networking.Requests.RequestOuterClass.Request;
import com.google.protobuf.ByteString;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.util.Crypto;

import java.util.Iterator;
import java.util.Random;

import net.jpountz.xxhash.StreamingXXHash32;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;

public class Signature {
    public Signature() {
    }

    public static void setSignature(Builder builder, long startTime) {
        if (builder.getAuthTicket() != null) {
            byte[] uk22 = new byte[32];
            new Random().nextBytes(uk22);

            long curTime = System.currentTimeMillis();
            byte[] authTicketBA = builder.getAuthTicket().toByteArray();
            POGOProtos.Networking.Envelopes.SignatureOuterClass.Signature.Builder sigBuilder =
                    POGOProtos.Networking.Envelopes.SignatureOuterClass.Signature.newBuilder().
                            setLocationHash1(
                                    getLocationHash1(authTicketBA)).
                            setLocationHash2(
                                    getLocationHash2()).
                            setUnk22(ByteString.copyFrom(uk22)).
                            setTimestamp(curTime).
                            setTimestampSinceStart(curTime - startTime);
//            DeviceInfo deviceInfo = api.getDeviceInfo();
//            if (deviceInfo != null) {
//                sigBuilder.setDeviceInfo(deviceInfo);
//            }

            Iterator uk2 = builder.getRequestsList().iterator();

            byte[] encrypted;
            while (uk2.hasNext()) {
                Request iv = (Request) uk2.next();
                encrypted = iv.toByteArray();
                sigBuilder.addRequestHash(getRequestHash(authTicketBA, encrypted));
            }

            byte[] uk21 = sigBuilder.build().toByteArray();
            byte[] iv1 = new byte[32];
            (new Random()).nextBytes(iv1);
            encrypted = Crypto.encrypt(uk21, iv1).toByteBuffer().array();
            Unknown6 uk6 = Unknown6.newBuilder().setRequestType(6).setUnknown2(Unknown2.newBuilder().setUnknown1(ByteString.copyFrom(encrypted))).build();
            builder.addUnknown6(uk6);
        }
    }

    private static byte[] getBytes(double input) {
        long rawDouble = Double.doubleToRawLongBits(input);
        return new byte[]{(byte) ((int) (rawDouble >>> 56)), (byte) ((int) (rawDouble >>> 48)), (byte) ((int) (rawDouble >>> 40)), (byte) ((int) (rawDouble >>> 32)), (byte) ((int) (rawDouble >>> 24)), (byte) ((int) (rawDouble >>> 16)), (byte) ((int) (rawDouble >>> 8)), (byte) ((int) rawDouble)};
    }

    private static int getLocationHash1(/*PokemonGo api, */byte[] authTicket/*, Builder builder*/) {
        XXHashFactory factory = XXHashFactory.fastestInstance();
        StreamingXXHash32 xx32 = factory.newStreamingHash32(461656632);
        xx32.update(authTicket, 0, authTicket.length);
        byte[] bytes = new byte[24];
        System.arraycopy(getBytes(0 /*api.getLatitude()*/), 0, bytes, 0, 8);
        System.arraycopy(getBytes(0 /*api.getLongitude()*/), 0, bytes, 8, 8);
        System.arraycopy(getBytes(0 /*api.getAltitude()*/), 0, bytes, 16, 8);
        xx32 = factory.newStreamingHash32(xx32.getValue());
        xx32.update(bytes, 0, bytes.length);
        return xx32.getValue();
    }

    private static int getLocationHash2(/*PokemonGo api, Builder builder*/) {
        XXHashFactory factory = XXHashFactory.fastestInstance();
        byte[] bytes = new byte[24];
        System.arraycopy(getBytes(0 /*api.getLatitude()*/), 0, bytes, 0, 8);
        System.arraycopy(getBytes(0 /*api.getLongitude()*/), 0, bytes, 8, 8);
        System.arraycopy(getBytes(0 /*api.getAltitude()*/), 0, bytes, 16, 8);
        StreamingXXHash32 xx32 = factory.newStreamingHash32(461656632);
        xx32.update(bytes, 0, bytes.length);
        return xx32.getValue();
    }

    private static long getRequestHash(byte[] authTicket, byte[] request) {
        XXHashFactory factory = XXHashFactory.fastestInstance();
        StreamingXXHash64 xx64 = factory.newStreamingHash64(461656632L);
        xx64.update(authTicket, 0, authTicket.length);
        xx64 = factory.newStreamingHash64(xx64.getValue());
        xx64.update(request, 0, request.length);
        return xx64.getValue();
    }
}
