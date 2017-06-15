package com.wynprice.entityHoppers;

import static net.minecraftforge.common.ForgeVersion.Status.AHEAD;
import static net.minecraftforge.common.ForgeVersion.Status.BETA;
import static net.minecraftforge.common.ForgeVersion.Status.BETA_OUTDATED;
import static net.minecraftforge.common.ForgeVersion.Status.OUTDATED;
import static net.minecraftforge.common.ForgeVersion.Status.PENDING;
import static net.minecraftforge.common.ForgeVersion.Status.UP_TO_DATE;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.ForgeVersion.Status;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.versioning.ComparableVersion;


public class UpdateChecker 
{
	Boolean doUpdate = true;
	@SubscribeEvent
	public void OnPlayerJoin(PlayerLoggedInEvent e) throws IOException
	{
		if(doUpdate)
		{
			doUpdate = false;
			Status status = PENDING;
	        ComparableVersion target = null;
			URL url = new URL(References.UPDATE_URL);
			InputStream con = url.openStream();
	        String data = new String(ByteStreams.toByteArray(con), "UTF-8");
	        con.close();
	        @SuppressWarnings("unchecked")
	        Map<String, Object> json = new Gson().fromJson(data, Map.class);
	        @SuppressWarnings("unchecked")
	        Map<String, String> promos = (Map<String, String>)json.get("promos");
	        String display_url = (String)json.get("homepage");

	        String rec = promos.get(MinecraftForge.MC_VERSION + "-recommended");
	        String lat = promos.get(MinecraftForge.MC_VERSION + "-latest");
	        ComparableVersion current = new ComparableVersion(References.VERSION);
	        if (rec != null)
	        {
	            ComparableVersion recommended = new ComparableVersion(rec);
	            int diff = recommended.compareTo(current);

	            if (diff == 0)
	                status = UP_TO_DATE;
	            else if (diff < 0)
	            {
	                status = AHEAD;
	                if (lat != null)
	                {
	                    ComparableVersion latest = new ComparableVersion(lat);
	                    if (current.compareTo(latest) < 0)
	                    {
	                        status = OUTDATED;
	                        target = latest;
	                    }
	                }
	            }
	            else
	            {
	                status = OUTDATED;
	                target = recommended;
	            }
	        }
	        else if (lat != null)
	        {
	            ComparableVersion latest = new ComparableVersion(lat);
	            if (current.compareTo(latest) < 0)
	            {
	                status = BETA_OUTDATED;
	                target = latest;
	            }
	            else
	                status = BETA;
	        }
	        else
	            status = BETA;
	        if(status == Status.OUTDATED)
	        	e.player.sendMessage((ITextComponent)  new TextComponentTranslation("version", References.VERSION, target));
			MainRegistry.getlogger().info("Update checker returned: " + status);
			
		}
	}
}
