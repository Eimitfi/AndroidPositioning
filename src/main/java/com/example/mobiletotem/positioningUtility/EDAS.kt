package com.example.mobiletotem.positioningUtility
import java.util.*
import kotlin.math.pow
import android.content.Context
import com.example.mobiletotem.ml.Model
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.lang.Exception

class EDAS(filePath: String,context: Context) {
    private val mesMan = MeasurementManager(filePath, context)
    private lateinit var lastMD:Point
    private val measurements:MutableMap<Int,Measurement> = mutableMapOf()

    fun getFilteredAnchors():List<Measurement>{
        val res:List<Measurement>
        val msrs = this.mesMan.getMeasurements()
        for(mes in msrs){
            if(!this.measurements.containsKey(mes.anchor) || this.measurements[mes.anchor]?.timestamp!! < mes.timestamp){
                this.measurements[mes.anchor] = mes
            }
        }
        val mes = this.measurements.toList().map { it.second }
        if(mes.size < 4){
            return listOf()
        }
        res = if(this::lastMD.isInitialized){
            ASA().asa(lastMD,mes)
        }else{
            ASA().asa(mes)
        }
        this.measurements.clear()
        println("selected anchors: ${res.map { it.anchor }}")
        return res
    }
    fun setLastMD(x:Double,y:Double){
        this.lastMD = Point(x,y)
    }
}

private class MeasurementManager(private val filePath: String, context: Context) {
    private val model: Model = Model.newInstance(context)
    private val inputFeature0:TensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1,135),DataType.FLOAT32)

    fun getMeasurements():List<Measurement> {
        val raw: List<List<String>> =  this.readMeasurements()
        val res: MutableList<Measurement> = mutableListOf()
        for(mes in raw){
            val array = FloatArray(135)
            for(i in 0.rangeTo(134)){
                array[i] = mes[6+i].toFloat()
            }
            inputFeature0.loadArray(array)
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
            res.add(Measurement(mes[0],mes[1],mes[2],mes[3],mes[4],mes[5],outputFeature0.floatArray[0].toString()))
        }
        return res
    }
    private fun readMeasurements():List<List<String>>{
        val res:List<List<String>> = try{
            val l = csvReader().readAll(File(filePath))
            File(filePath).delete()
            l
        }catch(e: Exception){
            listOf()
        }
        return res
    }

}
class Measurement(val anchor:Int,val x:Double,val y:Double,val z:Double,val timestamp:Long,val estdist:Double,val NLOS:Float) {
    constructor(anchor:String,x:String,y:String,z:String,timestamp:String,estdist:String,NLOS:String) : this(anchor.toInt(),x.toDouble(),y.toDouble(),z.toDouble(),timestamp.toLong(),estdist.toDouble(),NLOS.toFloat())
    override fun toString(): String {
        return "$anchor $x $y $z $timestamp $estdist $NLOS"
    }
}
private class ASA {
    fun asa(measurements:List<Measurement>):List<Measurement>{
        val combinations = this.getCombinations(measurements).map { Pair(listOf(it[0].anchor,it[1].anchor,it[2].anchor,it[3].anchor),it[0].NLOS+it[1].NLOS+it[2].NLOS+it[3].NLOS) }.sortedBy { it.second }
        val m0 = measurements.filter { it.anchor == combinations[0].first[0] }[0]
        val m1 = measurements.filter { it.anchor == combinations[0].first[1] }[0]
        val m2 = measurements.filter { it.anchor == combinations[0].first[2] }[0]
        val m3 = measurements.filter { it.anchor == combinations[0].first[3] }[0]
        return listOf(m0,m1,m2,m3)
    }
    fun asa(MD:Point,measurements: List<Measurement>):List<Measurement>{
        val combinations = this.getCombinations(measurements).map { Pair(listOf(it[0].anchor,it[1].anchor,it[2].anchor,it[3].anchor),it[0].NLOS+it[1].NLOS+it[2].NLOS+it[3].NLOS) }.sortedBy { it.second }
        for(i in combinations.indices){
            val m0 = measurements.filter { it.anchor == combinations[i].first[0] }[0]
            val m1 = measurements.filter { it.anchor == combinations[i].first[1] }[0]
            val m2 = measurements.filter { it.anchor == combinations[i].first[2] }[0]
            val m3 = measurements.filter { it.anchor == combinations[i].first[3] }[0]
            if(this.isInConvexHull(MD, listOf(Point(m0.x,m0.y),Point(m1.x,m1.y),Point(m2.x,m2.y),Point(m3.x,m3.y)))){
                return listOf(m0,m1,m2,m3)
            }
        }

        return this.asa(measurements)
    }
    private fun getCombinations(measurements: List<Measurement>):List<List<Measurement>>{
        //sisi brutto e poco approccio funzionale, lo so
        val res = mutableSetOf<MutableSet<Measurement>>()
        for(i in 0 until measurements.size-3){
            for(j in i+1 until measurements.size-2){
                for(z in i+2 until measurements.size-1){
                    for(u in i+3 until measurements.size){
                        res.add(mutableSetOf(measurements[i],measurements[j],measurements[z],measurements[u]))
                    }
                }
            }
        }
        res.removeIf { it.size != 4 }
        return res.map { it.toList() }.toList()
    }
    private fun isInConvexHull(checkPoint:Point, points: List<Point>): Boolean{
        val pts = mutableListOf<Point>()
        pts.add(checkPoint)
        pts.addAll(points)
        return convexHull(points) == convexHull(pts)
    }

    //dai i credits per questo codice trovato online
    /**
     * Finding the Convex Hull of a set of points.
     * Implementing [Graham scan algorithm](https://en.wikipedia.org/wiki/Graham_scan)
     *
     * @param points List of all the points in the plane
     * @return list of all the points included in the Convex Hull, in counterclockwise order
     */
    private fun convexHull(points: List<Point>): List<Point> {
        val anchor = findLowestYPoint(points)

        val sortedPoints = getListSortedByPolarAngleAndDistance(anchor, points)

        val hull = LinkedList<Point>()
        hull.addFirst(anchor)

        if (sortedPoints.size < 2) {
            hull.addAll(sortedPoints)
            return hull
        }
        hull.addLast(sortedPoints[0])

        for (i in 1 until sortedPoints.size) {
            while (pointNotCausingCounterClockwiseTurn(sortedPoints[i], hull)) {
                hull.removeLast()
            }
            hull.addLast(sortedPoints[i])
        }
        return hull
    }

    /**
     * Testing if adding a new point to the hull causes a clockwise turn or is collinear.
     * @param point Current point to be added to the hull
     * @param hull List containing all the previous points added to the hull
     *
     * @return true if the addition of the current point does not cause a counterclockwise shift. Else false
     */
    private fun pointNotCausingCounterClockwiseTurn(
        point: Point,
        hull: LinkedList<Point>
    ): Boolean {
        val top = hull.removeLast()
        val prevTop = hull.last()
        hull.addLast(top)

        val firstProduct = (top.x - prevTop.x) * (point.y - prevTop.y)
        val secondProduct = (top.y - prevTop.y) * (point.x - prevTop.x)
        val zCoordinate = firstProduct - secondProduct

        return when {
            zCoordinate > 0.0000 -> false
            else -> true
        }
    }

    /**
     * Sort the list of points by their distance and polar angle relative to the anchor point.
     * If any two point are of equal polar degree, the one nearest the anchor is removed
     *
     * @param anchor The starting point of the hull
     * @param points List of points, including the anchor point
     * @return sorted list of points, excluding the anchor point
     */
    private fun getListSortedByPolarAngleAndDistance(
        anchor: Point,
        points: List<Point>
    ): List<Point> {
        for (point in points) {
            point.anchorDistance = (point.x - anchor.x).pow(2.00) + (point.y - anchor.y).pow(2.00)
            point.polarAngle = kotlin.math.atan2((point.y - anchor.y), (point.x - anchor.x))
        }

        return points.asSequence()
            .filter { !(it.x == anchor.x && it.y == anchor.y) }
            .sortedWith(compareBy<Point> { it.polarAngle }.thenByDescending { it.anchorDistance })
            .distinctBy { it.polarAngle }
            .toList()
    }

    /**
     * Find the point with the lowest y value. If multiple occurrences, sort by lowest x value.
     * Used to decide the anchor point
     *
     * @param points List of points in the plane
     * @return point to be used as anchor point when calculating the hull
     */
    private fun findLowestYPoint(points: List<Point>): Point {
        var lowest = points[0]
        for (point in points) {
            when {
                point.y < lowest.y -> lowest = point
                point.y == lowest.y && point.x < lowest.x -> lowest = point
            }
        }
        return lowest
    }


}



private class Point(val x: Double, val y: Double) {
    var polarAngle = 0.00
    var anchorDistance = 0.00

    override fun equals(other:Any?):Boolean{
        return other is Point && other.x == x && other.y == y
    }

    override fun toString(): String {
        return "$x $y"
    }
}

