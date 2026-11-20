package com.desktopmail2025.dlm

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

object DlmExport {
    fun pdf(lines:List<String>):ByteArray{
        val doc=PdfDocument();val width=595;val height=842;val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.rgb(25,42,75);textSize=12f}
        var i=0;var pageNo=1
        while(i<lines.size){val page=doc.startPage(PdfDocument.PageInfo.Builder(width,height,pageNo++).create());var y=54f
            while(i<lines.size&&y<height-48){paint.textSize=if(i==0)20f else 12f;paint.typeface=if(i==0)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT;val line=lines[i];var start=0
                val maxChars=if(paint.textSize>15)42 else 88
                while(start<line.length){page.canvas.drawText(line.substring(start,(start+maxChars).coerceAtMost(line.length)),42f,y,paint);y+=18f;start+=maxChars}
                if(line.isEmpty())y+=8f;i++
            };doc.finishPage(page)
        }
        val out=ByteArrayOutputStream();doc.writeTo(out);doc.close();return out.toByteArray()
    }
    fun png(lines:List<String>):ByteArray{
        val width=1800;val lineHeight=52;val height=(lines.sumOf{maxOf(1,(it.length+99)/100)}*lineHeight+160).coerceIn(400,30000)
        val bmp=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);val canvas=Canvas(bmp);canvas.drawColor(android.graphics.Color.WHITE)
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.rgb(25,42,75);textSize=30f}
        var y=90f
        lines.forEachIndexed{index,line->p.textSize=if(index==0)48f else 30f;p.typeface=if(index==0)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            val chunks=line.chunked(100);chunks.forEach{if(y<height-40){canvas.drawText(it,65f,y,p);y+=lineHeight}}
        }
        val out=ByteArrayOutputStream();bmp.compress(Bitmap.CompressFormat.PNG,100,out);bmp.recycle();return out.toByteArray()
    }
}
