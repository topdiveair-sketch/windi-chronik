package at.zuhauseambach.einkauf;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
  private static final String OFFER_URL="https://raw.githubusercontent.com/topdiveair-sketch/windi-chronik/main/einkauf-angebote/angebote.json";
  private final String[] shops={"HOFER","PENNY","BILLA","SPAR"};
  private final String[] items={"Semmeln/Brötchen","Brot","Butter","Marmelade","Honig","Schinken","Aufschnitt","Käse","Eier","Joghurt","Milch","Orangensaft","Kaffee","Tee","Obst","Gemüse","Mineralwasser","Weizenbier","Canon PG-510 Schwarz","Canon CL-511 Farbe","Kopierpapier DIN A4"};
  private final String[] brands={"Backbox","Zurück zum Ursprung Bio-Toast","Milsani Irische Butter","Darbo","Natur aktiv","Ich bin Österreich","Ich bin Österreich Bratenaufschnitt","Schärdinger Mondseer","Zurück zum Ursprung","GAZI Ciftlik Naturjoghurt","Schärdinger Formil Vollmilch","Happy Day","Tchibo Feine Milde","Westminster","Obstangebot","Ich bin Österreich Rispentomaten","BILLA immer gut","Gösser NaturWeizen","Canon PG-510 Original","Canon CL-511 Original","Kopierpapier 80 g/m²"};
  private final String[] defaultShop={"HOFER","HOFER","HOFER","SPAR","HOFER","PENNY","PENNY","PENNY","HOFER","PENNY","PENNY","PENNY","PENNY","HOFER","HOFER","PENNY","BILLA","SPAR","SPAR","SPAR","SPAR"};
  private final int[] qty={20,1,2,1,1,2,2,1,12,1,2,2,1,1,2,1,8,10,1,1,1};
  private final String[] currentPrices={"0","1.49","1.69","0","0","0","2.99","4.49","0","1.89","0.89","0","7.99","0","0","2.39","0.55","0","0","0","0"};
  private LinearLayout list;
  private EditText guests,hosts,days,price;
  private TextView sum,status;
  private SharedPreferences prefs;

  public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("einkauf",0);build();if(!today().equals(prefs.getString("remoteDay","")))loadOffers(false);}
  private String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.GERMANY).format(new Date());}
  private TextView tv(String s,int sp,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setPadding(20,12,20,12);if(bold)v.setTypeface(Typeface.DEFAULT_BOLD);return v;}
  private EditText number(String key,String def){EditText e=new EditText(this);e.setInputType(2|8192);e.setText(prefs.getString(key,def));return e;}
  private void build(){ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,18,18,30);sc.addView(root);
    root.addView(tv("Einkauf Zuhause am Bach",26,true));root.addView(tv("Automatische Preisprüfung einmal täglich beim ersten Öffnen",13,false));root.addView(tv("Zwei-Tage-Planung · HOFER → PENNY → BILLA → SPAR",15,false));
    status=tv("Letzte Aktualisierung: "+prefs.getString("remoteUpdated","noch keine"),13,true);root.addView(status);
    Button refresh=new Button(this);refresh.setText("Preise jetzt aktualisieren");refresh.setOnClickListener(v->loadOffers(true));root.addView(refresh);
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);guests=number("guests","2");hosts=number("hosts","2");days=number("days","2");price=number("price","10");for(EditText e:new EditText[]{guests,hosts,days,price}){e.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));row.addView(e);}root.addView(row);root.addView(tv("Gäste | Gastgeber | Tage | € je Gast",12,false));
    Button calc=new Button(this);calc.setText("Einkaufsliste berechnen");root.addView(calc);sum=tv("",16,true);root.addView(sum);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);calc.setOnClickListener(v->{save();render();});render();setContentView(sc);
  }
  private void loadOffers(boolean manual){status.setText("Preise werden geladen …");new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(OFFER_URL+"?t="+System.currentTimeMillis()).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(12000);BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder s=new StringBuilder();String line;while((line=r.readLine())!=null)s.append(line);r.close();JSONObject root=new JSONObject(s.toString());JSONArray a=root.getJSONArray("offers");SharedPreferences.Editor ed=prefs.edit();for(int j=0;j<a.length();j++){JSONObject o=a.getJSONObject(j);String item=o.getString("item");for(int i=0;i<items.length;i++)if(items[i].equals(item)){ed.putString("b"+i,o.optString("brand",brands[i]));ed.putString("p"+i,o.optString("price",currentPrices[i]));ed.putString("s"+i,o.optString("shop",defaultShop[i]));ed.putString("sort"+i,o.optString("variety",""));ed.putString("normal"+i,o.optString("normalPrice",""));ed.putString("discount"+i,o.optString("discount",""));ed.putString("pack"+i,o.optString("package",""));ed.putString("total"+i,o.optString("totalQuantity",""));ed.putString("unit"+i,o.optString("unitPrice",""));ed.putString("deposit"+i,o.optString("deposit",""));ed.putString("valid"+i,o.optString("validUntil",root.optString("validFor","")));ed.putString("action"+i,o.optString("action",""));}}
      String updated=root.optString("updated",today());ed.putString("remoteDay",today()).putString("remoteUpdated",updated).apply();runOnUiThread(()->{status.setText("Aktualisiert: "+updated);render();Toast.makeText(this,"Preise aktualisiert",Toast.LENGTH_SHORT).show();});
    }catch(Exception e){runOnUiThread(()->{status.setText("Aktualisierung fehlgeschlagen – gespeicherte Preise bleiben erhalten");if(manual)Toast.makeText(this,"Keine Verbindung zur Angebotsdatei",Toast.LENGTH_LONG).show();});}}).start();}
  private int val(EditText e,int d){try{return Integer.parseInt(e.getText().toString());}catch(Exception x){return d;}}
  private void save(){prefs.edit().putString("guests",guests.getText().toString()).putString("hosts",hosts.getText().toString()).putString("days",days.getText().toString()).putString("price",price.getText().toString()).apply();}
  private String detail(int i){StringBuilder d=new StringBuilder();String variety=prefs.getString("sort"+i,"");String action=prefs.getString("action"+i,"");String normal=prefs.getString("normal"+i,"");String discount=prefs.getString("discount"+i,"");String pack=prefs.getString("pack"+i,"");String total=prefs.getString("total"+i,"");String unit=prefs.getString("unit"+i,"");String deposit=prefs.getString("deposit"+i,"");String valid=prefs.getString("valid"+i,"");String p=prefs.getString("p"+i,currentPrices[i]);if(!variety.isEmpty())d.append("Sorte: ").append(variety).append("\n");if(!action.isEmpty())d.append("Aktion: ").append(action).append("\n");if(!normal.isEmpty())d.append("Normalpreis: ").append(normal).append(" €\n");if(!discount.isEmpty())d.append("Rabatt: ").append(discount).append("\n");d.append("Aktionspreis: ").append(p.equals("0")||p.equals("0.00")?"Preis derzeit nicht verifiziert":p+" €").append("\n");if(!pack.isEmpty())d.append("Gebinde: ").append(pack).append("\n");if(!total.isEmpty())d.append("Gesamtmenge: ").append(total).append("\n");if(!unit.isEmpty())d.append("Grundpreis: ").append(unit).append("\n");if(!deposit.isEmpty())d.append("Pfand: ").append(deposit).append("\n");if(!valid.isEmpty())d.append("Gültig bis: ").append(valid);return d.toString();}
  private void render(){list.removeAllViews();int persons=val(guests,2)+val(hosts,2),d=val(days,2);double factor=(persons*d)/8.0,total=0;
    for(String shop:shops){TextView h=tv(shop,20,true);h.setBackgroundColor(0xffe8efe9);list.addView(h);for(int i=0;i<items.length;i++)if(prefs.getString("s"+i,defaultShop[i]).equals(shop)){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);CheckBox c=new CheckBox(this);c.setText(items[i]+" – "+prefs.getString("b"+i,brands[i]));r.addView(c);String details=detail(i);if(!details.isEmpty()){TextView info=tv(details,13,false);info.setBackgroundColor(0xfffff7dd);r.addView(info);}LinearLayout ed=new LinearLayout(this);ed.setOrientation(LinearLayout.HORIZONTAL);EditText q=number("q"+i,String.valueOf(Math.max(1,(int)Math.ceil(qty[i]*factor))));EditText br=new EditText(this);br.setText(prefs.getString("b"+i,brands[i]));EditText pr=number("p"+i,currentPrices[i]);for(EditText e:new EditText[]{q,br,pr}){e.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));ed.addView(e);}r.addView(ed);r.addView(tv("Menge | Marke/Hersteller | Preis €",11,false));list.addView(r);try{double pv=Double.parseDouble(pr.getText().toString().replace(',','.'));if(pv>0)total+=pv;}catch(Exception ignored){}int idx=i;q.setOnFocusChangeListener((v,f)->{if(!f)prefs.edit().putString("q"+idx,q.getText().toString()).apply();});br.setOnFocusChangeListener((v,f)->{if(!f)prefs.edit().putString("b"+idx,br.getText().toString()).apply();});pr.setOnFocusChangeListener((v,f)->{if(!f)prefs.edit().putString("p"+idx,pr.getText().toString()).apply();});}}
    double revenue=val(guests,2)*val(days,2)*val(price,10);sum.setText(String.format(Locale.GERMANY,"Frühstücksumsatz: %.2f € · bekannte Kosten: %.2f € · Differenz: %.2f €",revenue,total,revenue-total));
  }
}
