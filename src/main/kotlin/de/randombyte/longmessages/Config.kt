package de.randombyte.longmessages

import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable

@ConfigSerializable
internal class Config(
    @Setting val triggerCharacters: List<String> = listOf("+", "&"),
    @Setting(comment = "Normally it is only possible to use the trigger chars as suffixes " +
            "but when this is turned on they can be used as prefixes too")
    val triggerCharactersAsPrefix: Boolean = false
)