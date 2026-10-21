package dev.smokekitchen.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.smokekitchen.SmokeKitchen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.*;
import java.util.*;

/** World-time rain transitions persist even when every drying rack's chunk is unloaded. */
public final class DryWeatherHistory extends SavedData {
    public record Mark(long start,boolean raining){
        static final Codec<Mark> CODEC=RecordCodecBuilder.create(i->i.group(Codec.LONG.fieldOf("start").forGetter(Mark::start),Codec.BOOL.fieldOf("raining").forGetter(Mark::raining)).apply(i,Mark::new));
    }
    public static final Codec<DryWeatherHistory> CODEC=RecordCodecBuilder.create(i->i.group(Codec.LONG.fieldOf("through").forGetter((DryWeatherHistory v)->v.through),Mark.CODEC.listOf().fieldOf("marks").forGetter((DryWeatherHistory v)->v.marks)).apply(i,DryWeatherHistory::new));
    public static final SavedDataType<DryWeatherHistory> TYPE=new SavedDataType<>(SmokeKitchen.id("dry_weather"),DryWeatherHistory::new,CODEC,null);
    private long through;
    private final List<Mark> marks=new ArrayList<>();
    public DryWeatherHistory(){through=-1;}
    private DryWeatherHistory(long through,List<Mark> source){this.through=through;source.stream().filter(m->m.start>=0&&m.start<=through).sorted(Comparator.comparingLong(Mark::start)).skip(Math.max(0,source.size()-4096)).forEach(marks::add);}
    public static DryWeatherHistory get(ServerLevel level){var h=level.getDataStorage().computeIfAbsent(TYPE);h.observe(level.getGameTime(),level.isRaining());return h;}
    public void observe(long now,boolean raining){
        if(now<through){marks.clear();through=now;}
        if(marks.isEmpty()||marks.getLast().raining!=raining){marks.add(new Mark(now,raining));if(marks.size()>4096)marks.removeFirst();setDirty();}
        through=now;if(now%20==0)setDirty();
    }
    /** Unknown/evicted time is deliberately not credited as dry weather. */
    public long dryTicks(long from,long to){
        if(marks.isEmpty()||to<=from)return 0;long result=0;to=Math.min(to,through);from=Math.max(from,marks.getFirst().start);
        for(int n=0;n<marks.size();n++){var m=marks.get(n);long end=n+1<marks.size()?marks.get(n+1).start:through;if(!m.raining)result+=Math.max(0,Math.min(to,end)-Math.max(from,m.start));if(end>=to)break;}
        return result;
    }
}
