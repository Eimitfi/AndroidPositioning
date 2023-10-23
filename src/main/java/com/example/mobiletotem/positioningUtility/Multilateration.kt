package com.example.mobiletotem.positioningUtility
import kotlin.math.pow
class Multilateration {
    fun getPosition(measurements:List<Measurement>):Point3D{
        return this.trilateration(measurements[0],measurements[1],measurements[2])
    }
    private fun trilateration(p1:Measurement,p2:Measurement,p3:Measurement):Point3D{
        val v1 = Point3D(p2.x-p1.x,p2.y-p1.y,p2.z-p1.z)
        val h = (v1.x.pow(2)+v1.y.pow(2)+v1.z.pow(2)).pow(0.5)
        v1.division(h)
        val i = v1.x * (p3.x-p1.x) + v1.y * (p3.y-p1.y) + v1.z * (p3.z - p1.z)
        val v2 = Point3D(p3.x - p1.x - (i*v1.x),p3.y - p1.y - (i*v1.y),p3.z - p1.z - (i*v1.z))
        val t = (v2.x.pow(2)+v2.y.pow(2)+v2.z.pow(2)).pow(0.5)
        v2.division(t)
        val j = v2.x*(p3.x-p1.x)+v2.y*(p3.y-p1.y)+v2.z*(p3.z-p1.z)
        val v3 = Point3D(v1.y*v2.z-v1.z*v2.y,v1.z*v2.z-v1.x*v2.z,v1.x*v2.y-v1.y*v2.x)
        val u = (p1.estdist.pow(2)-p2.estdist.pow(2)+h.pow(2))/(2*h)
        val v = (p1.estdist.pow(2)-p3.estdist.pow(2)+i*(i-2*u)+j.pow(2))/(2*j)
        val w = if(p1.estdist.pow(2)-u.pow(2)-v.pow(2) < 0){
            ((p1.estdist.pow(2)-u.pow(2)-v.pow(2))*-1).pow(0.5)
        } else {
            (p1.estdist.pow(2)-u.pow(2)-v.pow(2)).pow(0.5)
        }
        return Point3D(p1.x+u*v1.x+v*v2.x+w*v3.x,p1.y+u*v1.y+v*v2.y+w*v3.y,p1.z+u*v1.z+v*v2.z+w*v3.z)
    }
}

class Point3D(var x:Double, var y:Double, var z:Double){
    fun division(n:Double){
        this.x = this.x / n
        this.y = this.y / n
        this.z = this.z / n
    }
    override fun equals(other:Any?):Boolean{
        return other is Point3D && other.x == x && other.y == y && other.z == z
    }

    override fun toString(): String {
        return "$x $y $z"
    }
}