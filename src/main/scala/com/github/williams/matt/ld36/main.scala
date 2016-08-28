package com.github.williams.matt.ld36

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.scalajs.js.JSApp
import scala.math.{ Pi, min, max }
import scala.util.Random
import org.scalajs.dom
import org.scalajs.dom.raw.{ HTMLElement, HTMLImageElement }
import org.denigma.threejs._
import org.denigma.threejs.extensions.SceneContainer
import org.denigma.threejs.extensions.controls.{ HoverControls, CameraControls }

@js.native
@JSName("THREE.AnimationMixer")
class AnimationMixer extends js.Object {
  def this(root: org.denigma.threejs.Scene = js.native) = this()
  def clipAction(clip: js.Any, root: js.Any): AnimationMixerAction = js.native
  def update(delta: Double): AnimationMixer = js.native
}

@js.native
@JSName("THREE.AnimationMixer._Action")
class AnimationMixerAction extends js.Object {
  def play(): AnimationMixerAction = js.native
  def stop(): AnimationMixerAction = js.native
  def reset(): AnimationMixerAction = js.native
  def startAt(time: Double): AnimationMixerAction = js.native
  def isRunning(): Boolean = js.native
  def setLoop(mode: Int, repetitions: Int): AnimationMixerAction = js.native
}

@js.native
@JSName("THREE")
object MyTHREE extends js.Object {
  var LoopOnce: Int = js.native
  var LoopRepeat: Int = js.native
  var LoopPingPong: Int = js.native
}

object Main extends JSApp {
  def main(): Unit = {
    val el: HTMLElement = dom.document.getElementById("container").asInstanceOf[HTMLElement]
/*
    def resize() = {
      el.width = dom.window.innerWidth;
      el.height = dom.window.innerHeight;
    }
    val scene = new Scene(el, el.width, el.height)
*/
    val scene = new Scene(el, dom.window.innerWidth, dom.window.innerHeight)
    scene.render()
  }
}

trait Container3D extends SceneContainer {
  container.style.position = "relative"

  override type RendererType = WebGLRenderer

  val absolute = "absolute"
  val positionZero = "0"

  protected def initRenderer() = {
    val params = js.Dynamic.literal(
      antialias = true
      ).asInstanceOf[WebGLRendererParameters]
    val vr = new WebGLRenderer(params)

    vr.domElement.style.position = absolute
    vr.domElement.style.top = positionZero
    vr.domElement.style.margin = positionZero
    vr.domElement.style.padding = positionZero
    vr.setSize(width, height)
    vr
  }

  val controls: CameraControls = new HoverControls(camera, this.container)

  container.appendChild(renderer.domElement)

  override def onEnterFrame(): Unit = {
    controls.update()
    renderer.render(scene, camera)
  }
}

class Golem(geometry: Geometry, material: MeshBasicMaterial, mixer: AnimationMixer) {
  private val mesh = new SkinnedMesh(geometry, material)
  mesh.scale.set(500, 500, 500);
  mesh.position.set(-100, -400, 0);

  val object3d = new Object3D()
  object3d.add(mesh)

  val position = object3d.position;
  var _orientation: Double = 0;
  val up: Vector3 = new Vector3(0, 1, 0)
  val forward: Vector3 = new Vector3(1, 0, 0)
  def orientation = _orientation;
  def orientation_= (value: Double):Unit = {
    object3d.rotateY(value - _orientation)
    forward.applyAxisAngle(up, value - _orientation);
    _orientation = value;
  }

  private val walkBegin = mixer.clipAction("walk.begin", mesh)
  private val walkCycle = mixer.clipAction("walk.cycle", mesh)
  private val walkEnd = mixer.clipAction("walk.end", mesh)

  private val target = new Vector3()
  def walkTo(x: Double, z: Double): Unit = {
    target.set(x, 0, z)
    if ((position.distanceTo(target) > 1) &&
        (!walkBegin.isRunning()) &&
        (!walkCycle.isRunning())) {
      walkEnd.stop();
      walkBegin.setLoop(MyTHREE.LoopOnce, 1).play();
      walkCycle.startAt(0.833333).play();
    }
  }

  def update(delta: Double): Unit = {
    if (walkBegin.isRunning() || walkCycle.isRunning() || walkEnd.isRunning()) {
      val bearingVector = target.clone().sub(position)
      val bearing = forward.angleTo(bearingVector)
      val bearingDelta = min(Pi / 32, bearing)
      val trialForward = forward.clone().applyAxisAngle(up, bearingDelta)
      if (trialForward.angleTo(bearingVector) > bearing) {
        orientation -= bearingDelta
      } else {
        orientation += bearingDelta
      }
      if ((position.distanceTo(target) < 600 * 1.66666) &&
          ((walkBegin.isRunning()) ||
           (walkCycle.isRunning()))) {
        walkBegin.stop();
        walkCycle.stop();
        walkEnd.play();
      }
      // object3d automatically considers forward vector when translating
      if (position.distanceTo(target) < 600 * delta) {
        walkEnd.stop();
        object3d.translateX(position.distanceTo(target));
      } else {
        object3d.translateX(600 * delta);
      }
    }
  }
}

class Scene(val container: HTMLElement, val width: Double, val height: Double) extends Container3D {
  val mixer = new AnimationMixer(scene)

  val textureLoader = new TextureLoader();
  val jsonLoader = new JSONLoader();
  jsonLoader.load("tunnel.json", (geometry: JSonLoaderResultGeometry, materials: js.Array[Material]) => {
    textureLoader.load("tunnel.png", (texture: Texture) => {
      val material = new MeshBasicMaterial(js.Dynamic.literal(
          map = texture
        ).asInstanceOf[MeshBasicMaterialParameters]);
      for (z <- -9 to 9) {
        val mesh = new Mesh(geometry, material);
        mesh.position.set(0, 0, z * -2000);
        mesh.scale.set(500, 500, 500);
        scene.add(mesh);
      }
    });
    ()
  });

  var golems: List[Golem] = List()

  jsonLoader.load("golem.json", (geometry: JSonLoaderResultGeometry, materials: js.Array[Material]) => {
    textureLoader.load("golem-good.png", (texture: Texture) => {
      val material = new MeshBasicMaterial(js.Dynamic.literal(
          map = texture,
          skinning = true
        ).asInstanceOf[MeshBasicMaterialParameters]);
      val anyGeometry = js.Dynamic.literal(geometry = geometry).geometry;
      for (i <- 0 to 2) {
        mixer.clipAction(anyGeometry.animations.selectDynamic(i.toString), ());
      }
      val golem = new Golem(geometry, material, mixer);
      scene.add(golem.object3d);
      golem.position.set(0, 0, -18000);
      //golem.orientation = -Pi / 2;
      golem.walkTo(0, -2000)
      golems = golem :: golems;

      val golem2 = new Golem(geometry, material, mixer);
      scene.add(golem2.object3d);
      golem2.position.set(0, 0, 18000);
      golem2.orientation = Pi / 2;
      golem2.walkTo(0, 2000)
      golems = golem2 :: golems;
    });
    ()
  });

  val light = new DirectionalLight(0xffffff, 2)
  light.position.set(1, 1, 1).normalize()
  scene.add(light)

  override def onEnterFrame(): Unit = {
    mixer.update(0.03)
    for (golem <- golems) {
      golem.update(0.03)
    }
    
    super.onEnterFrame()
  }
}
