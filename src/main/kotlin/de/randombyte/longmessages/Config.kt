package de.randombyte.longmessages

import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable

@ConfigSerializable
internal class Config(
    @Setting val triggerCharacters: List<String> = listOf("+", "&"),
    @Setting val triggerCharactersAsPrefix: Boolean = true
)