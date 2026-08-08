package com.bitiplay.world.ent

import com.bitiplay.world.art.C
import com.bitiplay.world.audio.Sfx
import com.bitiplay.world.audio.Snd
import com.bitiplay.world.core.rnd
import com.bitiplay.world.core.rndInt
import com.bitiplay.world.engine.Scene
import kotlin.math.abs

/** Background residents. Same rig as a playable character, driven by a timer. */
object NpcRoster {
    val all = listOf(
        CharSpec("Olive", 0xFF7FBF5A.toInt(), 0xFFE9F5CC.toInt(), Ear.NONE, Tail.NONE, Snout.ROUND),
        CharSpec("Momo", 0xFFA98BD8.toInt(), 0xFFEDE2F8.toInt(), Ear.ROUND, Tail.LONG, Snout.ROUND),
        CharSpec("Sunny", 0xFFF5D060.toInt(), 0xFFFFF4CC.toInt(), Ear.NONE, Tail.SHORT, Snout.BEAK),
        CharSpec("Nutmeg", 0xFFD9743F.toInt(), 0xFFF6E2CE.toInt(), Ear.POINTY, Tail.BUSHY, Snout.CAT),
        CharSpec("Pebble", 0xFFA6AEB8.toInt(), 0xFFE9EDF2.toInt(), Ear.ROUND, Tail.NONE, Snout.ROUND),
        CharSpec("Basil", 0xFF5FBFA8.toInt(), 0xFFD8F2E8.toInt(), Ear.TINY, Tail.SHORT, Snout.ROUND)
    )

    fun spec(i: Int): CharSpec = all[i.mod(all.size)]
}

/**
 * Wanders, greets animals, and occasionally carries something about.
 * Deliberately never touches tools or vehicles, so it cannot walk off with
 * whatever the player was about to use.
 */
class Npc(spec: CharSpec, startX: Float, private val roam: Float = 640f) :
    Character(spec, -1) {

    override val playable: Boolean get() = false

    private val homeX = startX
    private var aiT = rnd(0.4f, 2.6f)
    private var holdT = 0f

    init {
        x = startX
        depth = rnd(0f, DEPTH_BAND)
        y = depth
        speed = rnd(150f, 215f)
        facing = if (rndInt(0, 2) == 0) -1 else 1
    }

    override fun caption(ch: Character): String = "say hello to ${spec.name}"

    override fun onUse(ch: Character, scene: Scene): Boolean {
        ch.faceToward(scene, x)
        faceToward(scene, ch.x)
        ch.startAct(Act.CHEER, 0.6f)
        startAct(Act.CHEER, 0.6f)
        scene.fx.hearts(x, -170f, level, 3)
        Sfx.play(Snd.HAPPY)
        stop()
        aiT = 1.4f
        return true
    }

    override fun update(dt: Float, scene: Scene) {
        super.update(dt, scene)
        if (dead) return

        if (holdT > 0f) {
            holdT -= dt
            if (holdT <= 0f && carried != null) dropCarried(scene)
        }

        aiT -= dt
        if (aiT > 0f || targetX != null || busy) return
        aiT = rnd(1.5f, 4.2f)

        // Visiting an animal is the noisy option, so it is now half as likely;
        // the freed weight goes to silent wandering.
        when (rndInt(0, 10)) {
            in 0..5 -> wander(scene)
            in 6..7 -> {
                startAct(Act.CHEER, 0.6f)
                scene.fx.notes(x, -180f, level, 2)
            }
            8 -> visitAnimal(scene)
            else -> fetchSomething(scene)
        }
    }

    private fun wander(scene: Scene) {
        goTo(scene, scene.wrapX(homeX + rnd(-roam, roam)), rnd(0f, DEPTH_BAND))
    }

    private fun visitAnimal(scene: Scene) {
        var best: Animal? = null
        var bestD = 950f
        for (i in scene.entities.indices) {
            val e = scene.entities[i]
            if (e is Animal && e.level == level) {
                val d = abs(scene.delta(x, e.x))
                if (d < bestD) {
                    bestD = d
                    best = e
                }
            }
        }
        val target = best
        if (target == null) {
            wander(scene)
            return
        }
        goTo(scene, scene.wrapX(target.x - 130f), 0f) { ch ->
            ch.faceToward(scene, target.x)
            target.onUse(ch, scene)
        }
    }

    private fun fetchSomething(scene: Scene) {
        if (carried != null) {
            dropCarried(scene)
            return
        }
        var best: Item? = null
        var bestD = 760f
        for (i in scene.entities.indices) {
            val e = scene.entities[i]
            if (e is Item && e.level == level && e.holder == null && !e.stored && !e.kind.tool) {
                val d = abs(scene.delta(x, e.x))
                if (d < bestD) {
                    bestD = d
                    best = e
                }
            }
        }
        val target = best
        if (target == null) {
            wander(scene)
            return
        }
        goTo(scene, scene.wrapX(target.x), target.baseY) { ch ->
            if (target.holder == null && ch.carried == null) {
                ch.pickUp(target, scene)
                holdT = rnd(5f, 11f)
                scene.fx.sparkles(target.x, target.y, level, 4, C.WHITE)
            }
        }
    }
}
