package de.randombyte.longmessages

import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable

@ConfigSerializable
internal class Config(
    @Setting val appendCharacters: List<String> = listOf("+", "...")
)