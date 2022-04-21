package com.pygostylia.osprey

import org.json.JSONObject

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.{IterableHasAsScala, IteratorHasAsScala}

object BlockState {
  private def blockInit(): Map[String, Int] = {
    val registry = new JSONObject(Files.readString(Path.of("generated/reports/registries.json")));
    val blockIds = registry.getJSONObject("minecraft:block").getJSONObject("entries");
    blockIds.keys().asScala.map({blockName =>
      (blockName, blockIds.getJSONObject(blockName).getInt("protocol_id"))
    }).toMap
  }

  private def init(): Seq[BlockState] = {
    val blocks = new JSONObject(Files.readString(Path.of("generated/reports/blocks.json")));

    blocks.keys().asScala.flatMap({ key =>
      val value = blocks.getJSONObject(key)
      importBlock(key, value)
    }).toSeq
  }

  private def importBlock(name: String, block: JSONObject): Seq[BlockState] = {
    val blockStates = block.getJSONArray("states")
    blockStates.asScala.map({state =>
      importState(name, state.asInstanceOf[JSONObject])
    }).toSeq
  }

  private def importState(name: String, state: JSONObject): BlockState = {
    val props = if (state.has("properties")) {
      val properties = state.getJSONObject("properties")
      properties.keys().asScala.map({ key =>
        val value = properties.getString(key)
        (key, value)
      }).toMap
    } else {
      null
    }
    val id = state.getInt("id")
    val default = state.has("default")
    new BlockState(name, props, id, default)
  }

  private val states: Seq[BlockState] = init()
  private val defaultStates: Map[String, BlockState] =
    states
      .filter({s => s.default})
      .map({s => (s.blockName, s)})
      .toMap
  private val protocolIds: Map[String, Int] = blockInit()
  private val protocolNames: Map[Int, String] = protocolIds.map({case (k, v) => (v, k)})

  def fromString(s: String): Option[BlockState] = {
    val justName = raw"(\w*:\w*)".r
    val nameProp = raw"(\w*:\w*)\[(.*)]".r
    val (name, props) = s match {
      case justName(name) => (name, null)
      case nameProp(name, props) =>
        (name,
          props
            .split(',')
            .map({
              p =>
                p.split("=", 2)
                  match { case Array(a, b) => (a, b) }})
            .toMap
        )
      case _ =>
        println("does not match")
        return None
    }
    states.find({state =>
      state.blockName == name && state.props == props
    })
  }
}

final case class BlockState(blockName: String, props: Map[String, String], protocolId: Int, default: Boolean) {
  override def toString: String = s"$blockName[${props.map({case (k, v) => s"$k=$v"}).mkString(",")}]"
}