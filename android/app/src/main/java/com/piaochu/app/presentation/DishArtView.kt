package com.piaochu.app.presentation
import android.content.Context
import android.graphics.*
import android.view.View
/** 本地矢量食物插画，离线可用；不依赖图片外链，也不把示意图当实物菜品照片。 */
class DishArtView(context:Context,private val kind:String):View(context){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    init{importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO}
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas);canvas.save();canvas.scale(width/120f,height/120f)
        fun oval(color:Int,l:Float,t:Float,r:Float,b:Float){paint.color=color;canvas.drawOval(l,t,r,b,paint)}
        oval(0x16000000,12f,91f,108f,108f)
        paint.color=0xFF204D3C.toInt();val bowl=Path().apply{moveTo(12f,55f);cubicTo(17f,112f,103f,112f,108f,55f);close()};canvas.drawPath(bowl,paint)
        oval(0xFFFCF8F1.toInt(),10f,38f,110f,77f)
        val broth=when(kind){"greens"->0xFF8DAD69.toInt();"steam"->0xFFE5C388.toInt();"soup"->0xFFE4B55E.toInt();"fish"->0xFFD58A42.toInt();else->0xFFB74C2F.toInt()}
        oval(broth,17f,44f,103f,71f)
        for(i in 0..6){val x=28f+(i%4)*18f;val y=48f+(i/4)*10f;oval(if(i%2==0)0xFFF2D7A2.toInt()else 0xFF668B45.toInt(),x,y,x+14,y+9)}
        paint.color=0xFFFCF8F1.toInt();paint.style=Paint.Style.STROKE;paint.strokeWidth=2f
        for(x in listOf(42f,62f,82f)){val p=Path().apply{moveTo(x,33f);cubicTo(x-10,22f,x+10,19f,x,8f)};canvas.drawPath(p,paint)}
        paint.style=Paint.Style.FILL;paint.color=0xFF9B6A3F.toInt();canvas.save();canvas.rotate(-18f,90f,40f);canvas.drawRoundRect(85f,9f,88f,64f,2f,2f,paint);canvas.drawRoundRect(94f,9f,97f,64f,2f,2f,paint);canvas.restore();canvas.restore()
    }
}
