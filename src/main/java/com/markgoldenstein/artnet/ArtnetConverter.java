package com.markgoldenstein.artnet;

import ch.bildspur.artnet.ArtNetClient;
import ch.bildspur.artnet.NodeStyle;
import ch.bildspur.artnet.PortDescriptor;
import ch.bildspur.artnet.events.ArtNetServerEventAdapter;
import ch.bildspur.artnet.packets.ArtDmxPacket;
import ch.bildspur.artnet.packets.ArtNetPacket;
import ch.bildspur.artnet.packets.ArtPollReplyPacket;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static ch.bildspur.artnet.packets.PacketType.ART_OUTPUT;
import static ch.bildspur.artnet.packets.PacketType.ART_POLL;

@CommandLine.Command(name = "artnet-converter", mixinStandardHelpOptions = true, version = "Art-Net Converter 0.1")
public class ArtnetConverter implements Runnable {
    @Parameters(index = "0", description = "Own Art-Net address")
    private InetAddress ownAddress;

    @Parameters(index = "1", description = "Target Art-Net address")
    private InetAddress targetHost;

    @Parameters(index = "2..*", description = "Channel conversions; format: channel:min:max")
    private String[] conversions;

    public void run() {
        Set<int[]> parsedConversions = new HashSet<>();

        for (String s : conversions) {
            int[] values = Arrays.stream(s.split(":")).mapToInt(Integer::parseInt).toArray();
            assert values.length == 3;
            assert values[0] >= 1 && values[0] <= 512;
            assert values[1] >= 0 && values[1] <= 255;
            assert values[2] >= 0 && values[2] <= 255;
            assert values[1] <= values[2];

            parsedConversions.add(new int[]{values[0], values[1], values[2]});
        }

        ArtNetClient artnet = new ArtNetClient();

        artnet.getArtNetServer().addListener(new ArtNetServerEventAdapter() {
            @Override
            public void artNetPacketReceived(ArtNetPacket packet) {
                if (packet.getType() == ART_POLL) {
                    ArtPollReplyPacket pollReplyPacket = buildPollReplyPacket();
                    pollReplyPacket.translateData();
                    artnet.getArtNetServer().broadcastPacket(pollReplyPacket);
                } else if (packet.getType() == ART_OUTPUT) {
                    ArtDmxPacket dmxPacket = (ArtDmxPacket) packet;
                    byte[] dmxData = dmxPacket.getDmxData();

                    for (int[] c : parsedConversions) {
                        double oldValue = dmxData[c[0] - 1] & 0xFF;
                        double newValue = oldValue / 255 * (c[2] - c[1]) + c[1];
                        dmxData[c[0] - 1] = (byte) newValue;
                    }

                    dmxPacket.setDMX(dmxData, 512);
                    artnet.getArtNetServer().unicastPacket(dmxPacket, targetHost);
                }
            }
        });

        artnet.start();
    }

    private ArtPollReplyPacket buildPollReplyPacket() {
        ArtPollReplyPacket pollReplyPacket = new ArtPollReplyPacket();
        pollReplyPacket.setIp(ownAddress);
        pollReplyPacket.setDmxIns(new byte[]{0, 0, 0, 0});
        pollReplyPacket.setDmxOuts(new byte[]{0, 0, 0, 0});
        pollReplyPacket.setVersionInfo(1);
        pollReplyPacket.setSubSwitch(0);
        pollReplyPacket.setOemCode(400);
        pollReplyPacket.setNodeStatus(2);
        pollReplyPacket.setUbeaVersion(0);
        pollReplyPacket.setEstaManufacturerCode(17742);
        pollReplyPacket.setShortName("Art-Net Converter");
        pollReplyPacket.setLongName("Art-Net Converter");
        pollReplyPacket.setNumPorts(1);
        pollReplyPacket.setPorts(new PortDescriptor[]{new PortDescriptor(192)});
        pollReplyPacket.setNodeStyle(NodeStyle.ST_NODE);
        return pollReplyPacket;
    }

    public static void main(String[] args) {
        new CommandLine(new ArtnetConverter()).execute(args);
    }
}
