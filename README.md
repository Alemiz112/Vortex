# StarGate Vortex
[![Discord Chat](https://img.shields.io/discord/767330242078834712.svg)](https://discord.gg/QcRRzXX)
> Vortex is a standalone message-broker implementation based on StarGate project

## Use Case
StarGate Vortex can be used in several different scenarios when communication between nodes is required.
The original idea comes from the Minecraft network scenario where multiple transfer-proxies (master nodes) needs to communicate
with downstream servers (child nodes) in order to exchange player and network information. However, Vortex is not focused directly at Minecraft 
and can be applied in different situations too.

### Architecture
Vortex is based on StarGate implementation and therefore migration from StarGate shouldn't be difficult. 
In addition Vortex introduces ZStandard compression in order to decrease the bandwith.  
Each connection has assigned *node* which is the main component of the network. Each node can send and receive *VortexMessage* and take an action depends on it.
*Master Node* should be superior to the *Child Nodes*. By default, when master node sends message every child will receive it. Child node can target the message to 
exact master node or can send message to all master nodes.  
Using rich API end users can create own implementation of messages or even fully customized *Vortex Packets*.

### Minecraft
For Minecraft there is `minecraft` module which contains custom implementation of child, master nodes and implements basic packets.  
Users can refer to this module as an example.

## Usage
### VortexSettings
This class holds the default, common information for client and server side nodes.

| Config option | Description |
| :---: | :---: |
| password | StarGate password used for initial authentication | 
| compression | The compression method to be used (CompressionEnum instance) | 
| compressionLevel | The compression level to be used (1 by default) | 
| port | The TCP port which is used for the StarGate connection |

### VortexClientSettings
Holds client connection specifyc options.

| Config option | Description |
| :---: | :---: |
| clientName | Unique identifier used for StarGate and Vortex service | 
| remoteAddress | The target address of the Stargate server | 
| protocolversion | Version of the StarGate protocol | 
| vortexType | Vortex node implementation type (e.g "vortex-master", "vortex-node") | 
| primaryMasterNode | The prefered master node. Leave empty if node doesn't have any master node or all master nodes have equal priority. | 
| masterNodes | List of the names of master nodes which child node will be assigned to. Leave empty if  node doesn't have any master node. |

### Compression
Vortex offers configurable and dynamic compression of the packets.  
Currently implemented compression methods:

| Method Name | Description |
| :---: | :---: |
| NO_COMPRESS | No compression, uncompressed data will be sent | 
| ZSTANDARD | Implements ZStandart compression which is used for compressing packets at StarGate entry level. |

### Logging
Vortex uses Log4J2 logger api. When creating own application `log4j-core` must be present in order to function properly.  
If trace log level is enabled, StarGate will log every packet with log level `LEVEL_FILTERED`. While testing it is recommended to use *debug* log level.

## Used Libraries
- [StarGate](https://github.com/Alemiz112/StarGate)
- [Log4J2](https://logging.apache.org/log4j/2.x/)
- [ZSTD JNI](https://github.com/luben/zstd-jni)