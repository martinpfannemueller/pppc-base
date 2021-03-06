#Basic BASE plug-ins.

##Introduction

This page contains a list of the available BASE plug-ins that are part of the distribution. All plug-ins are strictly optional, however, in order to be useful, BASE requires at least one complete stack of plug-ins. A complete stack usually encompasses a discovery plug-in, a transceiver plug-in, a serializer plug-in and a semantic plug-in. 

In addition, the page also lists a number of ultility classes that can simplify the development of additional plug-ins. The utility classes primarily focus on connection multiplexing and group-based communication.

##Plug-ins

The list below outlines all plug-ins that are part of the BASE distribution to date. The plug-ins are distributed over a set of projects to simplify compilation for different platforms.

*Project* | *Name* | *Type* | *Platform* | *Description*
----------|--------|--------|------------|--------------
base-plugin-common | !ProactiveDiscovery | Discovery | Any | A device discovery plug-in that distributes device and plug-in descriptions periodically.
base-plugin-common | !ProactiveRouting | Routing | Any | A plug-in that uses source-routing with proactive n-hop route distribution to enable multihop routing within a smart peer group.
base-plugin-common | RMISemantic | Semantic | Any | A plug-in that enables request-response interaction with at-most-once semantic.
base-plugin-common | !StreamSemantic | Semantic | Any | A plug-in that enables technology-independent connection-oriented communication between application objects.
base-plugin-common | !ObjectSerializer | Serializer | Any | A plug-in that marshalls and unmarshalls Java objects.
base-plugin-compression| GZIPCompressor | Compression | Any | A modifier plug-in that compresses byte streams using GZIP
base-plugin-android| !MxBluetoothTransceiver | Transceiver | Android 2.1 | A Bluetooth transceiver for Android devices that uses connection multiplexing.
base-plugin-bluetooth | !MxBluetoothTransceiver | Transceiver | Any, JSR-82 | A Bluetooth transceiver relying on JSR-82 that uses connection multiplexing.
base-plugin-emulator | MxIPEmulatorTransceiver | Transceiver | Any, IP | A transceiver plug-in to emulate different network topologies using multiplexed IP connections.
base-plugin-ip | IPBroadcastTransceiver | Transceiver | Any, IP | A transceiver plug-in that uses IP broadcast for group communication.
base-plugin-ip | MxIPBroadcastTransceiver | Transceiver | Any, IP | An extended IPBroadcastTransceiver that uses connection multiplexing.
base-plugin-ip | IPMulticastTransceiver | Transceiver | Any, IP | A transceiver plug-in that uses IP multicast for group communication.
base-plugin-ip | MxIPMulticastTransceiver | Transceiver | Any, IP | An extended IPMulticastTransceiver that uses connection multiplexing.
base-plugin-ip | !ProactiveRoutingGateway | Routing | Any, IP | An extended !ProactiveRouting plug-in that provides routing beyond smart peer groups through a routing server on the Internet.
base-plugin-irda | MxIRTransceiver | Transceiver | Win XP/CE | A transceiver plug-in that uses !WinSock to establish multiplexed connections over IRDA.
base-plugin-security | !SecureModifier | Encryption | Any, Bouncycastle | An plug-in that uses Bouncycastle to encrypt communication via AES. |
base-plugin-security | !ExchangeSemantic | Semantic | Any, Bouncycastle | A plug-in that uses Bouncycastle to enable automatic key-exchange via different key-exchange protocols.
base-plugin-serial | !MxSerialTransceiver | Transceiver | Any, Rxtx | A transceiver plug-in that uses the RXTX library to provide multiplexed interaction over a serial connection.
base-plugin-sunspot | !MxSerialTransceiver | Transceiver | SunSPOT | A transceiver plug-in that enables multiplexed communciation via the serial port of a SunSPOT.
base-plugin-sunspot | !MxSpotTransceiver | Transceiver | SunSPOT | A transceiver plug-in that enables multiplexed communication via the 802.15.4 interface of a SunSPOT.

##Utilities

The *base-core-system* project and the *base-plugin-common* project contain a number of utility classes that simplify plug-in development. All utility classes adhere to the J2ME CLDC specification and do not introduce further dependencies.

*Project* | *Name* | *Type* | *Platform* | *Descripton*
----------|--------|--------|------------|-------------
base-core-system | !FragmentConnector | Connector | Any | A connector that provides fragmentation for packets that exceed the length of the underlying connector.
base-core-system | !GroupConnector | Connector | Any | A connector that multiplexes a single connector into different groups by attaching an additional header.
base-plugin-common | !FloodConnector | Connector | Any | A connector that implements scoped flooding of packets sent via the connector.
base-plugin-common | !TimeoutConnector | Connector | Any | A connector that implements a simple synchronization protocol for unbuffered connections.
base-plugin-common | !MultiplexFactory | Multiplexer | Any | A multiplexer that can multiplex a single stream to provide connection-oriented and packet-based communication over it.
 