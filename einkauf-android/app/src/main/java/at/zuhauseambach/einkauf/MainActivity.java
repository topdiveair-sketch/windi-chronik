package at.zuhauseambach.einkauf;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
  private static final String OFFER_URL="https://raw.githubusercontent.com/topdiveair-sketch/windi-chronik/main/einkauf-angebote/angebote.json";
  private final String[] shops={"HOFER","PENNY","BILLA","SPAR"};
  private final String[] items={"Semmeln/Brötchen","Brot","Butter","Marmelade","Honig","Schinken","Aufschnitt","Käse","Eier","Joghurt","Milch","Orangensaft","Kaffee","Tee","Obst","Gemüse","Mineralwasser","Weizenbier","Weißwein Niederösterreich/Wachau","Roséwein Niederösterreich/Wachau","Rotwein Niederösterreich/Wachau","Canon PG-510 Schwarz","Canon CL-511 Farbe","Kopierpapier DIN A4"};
  private final String[] brands={"Backbox","Zurück zum Ursprung Bio-Toast","Milsani Irische Butter","Darbo","Natur aktiv","Ich bin Österreich","Ich bin Österreich Bratenaufschnitt","Schärdinger Mondseer","Zurück zum Ursprung","GAZI Ciftlik Naturjoghurt","Schärdinger Formil Vollmilch","Happy Day","Tchibo Feine Milde","Westminster","Obstangebot","Ich bin Österreich Rispentomaten","BILLA immer gut","Gösser NaturWeizen","Wachauer Weißwein","Wachauer Rosé","Niederösterreichischer Rotwein","Canon PG-510 Original","Canon CL-511 Original","Kopierpapier 80 g/m²"};
  private final String[] defaultShop={"HOFER","HOFER","HOFER","SPAR","HOFER","PENNY","PENNY","PENNY","HOFER","PENNY","PENNY","PENNY","PENNY","HOFER","HOFER","PENNY","BILLA","SPAR","SPAR","SPAR","SPAR","SPAR","SPAR","SPAR"};
  private final int[] qty={20,1,2,1,1,2,2,1,12,1,2,2,1,1,2,1,8,10,1,1,1,1,1,1};
  private final String[] currentPrices={"0","1.49","1.69","0","0","0","2.99","4.49","0","1.89","0.89","0","7.99","0","0","2.39","0.55","0","0","0","0","0","0","0"};
  private final int GREEN=Color.rgb(44,91,61), LIGHT=Color.rgb(246,248,245), GOLD=Color.rgb(190,143,47), TEXT=Color.rgb(33,43,36);
  private LinearLayout list;
  private EditText guests,hosts,days,price;
  private TextView sum,status;
  private SharedPreferences prefs;

  public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("einkauf",0);build();if(!today().equals(prefs.getString("remoteDay","")))loadOffers(false);}
  private String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.GERMANY).format(new Date());}
  private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
  private GradientDrawable bg(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp((int)radius));return g;}
  private TextView tv(String s,int sp,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextColor(TEXT);v.setTextSize(sp);v.setPadding(dp(14),dp(9),dp(14),dp(9));if(bold)v.setTypeface(Typeface.DEFAULT_BOLD);return v;}
  private TextView label(String s){TextView v=tv(s,12,true);v.setTextColor(Color.rgb(90,105,95));return v;}
  private EditText number(String key,String def){EditText e=new EditText(this);e.setInputType(2|8192);e.setText(prefs.getString(key,def));e.setTextColor(TEXT);e.setTextSize(16);e.setGravity(Gravity.CENTER);e.setBackground(bg(Color.WHITE,12));e.setPadding(dp(8),dp(10),dp(8),dp(10));return e;}
  private Button button(String text,int color){Button b=new Button(this);b.setText(text);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT_BOLD);b.setBackground(bg(color,14));b.setPadding(dp(10),dp(10),dp(10),dp(10));return b;}
  private LinearLayout.LayoutParams mp(int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(top),0,0);return p;}

  private void build(){
    ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(LIGHT);
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(34));sc.addView(root);

    LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(18),dp(18),dp(18),dp(18));hero.setBackground(bg(GREEN,20));
    TextView title=tv("Einkauf Zuhause am Bach",26,true);title.setTextColor(Color.WHITE);title.setPadding(0,0,0,dp(4));hero.addView(title);
    TextView sub=tv("Täglich günstig einkaufen · Melk und Umgebung",14,false);sub.setTextColor(Color.rgb(226,239,229));sub.setPadding(0,0,0,dp(4));hero.addView(sub);
    TextView route=tv("HOFER  →  PENNY  →  BILLA  →  SPAR",15,true);route.setTextColor(Color.WHITE);route.setPadding(0,dp(6),0,0);hero.addView(route);root.addView(hero);

    status=tv("Letzte Aktualisierung: "+prefs.getString("remoteUpdated","noch keine"),13,true);status.setBackground(bg(Color.WHITE,14));status.setLayoutParams(mp(12));root.addView(status);

    LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setGravity(Gravity.CENTER);actions.setLayoutParams(mp(10));
    Button refresh=button("↻ Preise aktualisieren",GREEN);Button print=button("🖨 Liste drucken / PDF",GOLD);
    LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,-2,1);half.setMargins(0,0,dp(6),0);refresh.setLayoutParams(half);LinearLayout.LayoutParams half2=new LinearLayout.LayoutParams(0,-2,1);half2.setMargins(dp(6),0,0,0);print.setLayoutParams(half2);actions.addView(refresh);actions.addView(print);root.addView(actions);
    refresh.setOnClickListener(v->loadOffers(true));print.setOnClickListener(v->printList());

    LinearLayout settings=new LinearLayout(this);settings.setOrientation(LinearLayout.VERTICAL);settings.setPadding(dp(14),dp(14),dp(14),dp(14));settings.setBackground(bg(Color.WHITE,18));settings.setLayoutParams(mp(12));
    settings.addView(tv("Frühstücksplanung",18,true));
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER);guests=number("guests","2");hosts=number("hosts","2");days=number("days","2");price=number("price","10");String[] labs={"Gäste","Gastgeber","Tage","€ je Gast"};EditText[] fields={guests,hosts,days,price};
    for(int i=0;i<fields.length;i++){LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.setGravity(Gravity.CENTER);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.setMargins(dp(3),0,dp(3),0);col.setLayoutParams(cp);TextView l=label(labs[i]);l.setGravity(Gravity.CENTER);col.addView(l);fields[i].setLayoutParams(new LinearLayout.LayoutParams(-1,-2));col.addView(fields[i]);row.addView(col);}settings.addView(row);
    Button calc=button("Einkaufsliste neu berechnen",GREEN);calc.setLayoutParams(mp(12));settings.addView(calc);root.addView(settings);

    sum=tv("",16,true);sum.setBackground(bg(Color.rgb(232,241,234),16));sum.setLayoutParams(mp(12));root.addView(sum);
    TextView heading=tv("Einkaufsliste",22,true);heading.setPadding(dp(4),dp(18),dp(4),dp(8));root.addView(heading);
    list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);
    calc.setOnClickListener(v->{save();render();});render();setContentView(sc);
  }

  private void loadOffers(boolean manual){status.setText("Preise werden geladen …");new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(OFFER_URL+"?t="+System.currentTimeMillis()).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(12000);BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder s=new StringBuilder();String line;while((line=r.readLine())!=null)s.append(line);r.close();JSONObject root=new JSONObject(s.toString());JSONArray a=root.getJSONArray("offers");SharedPreferences.Editor ed=prefs.edit();for(int j=0;j<a.length();j++){JSONObject o=a.getJSONObject(j);String item=o.getString("item");for(int i=0;i<items.length;i++)if(items[i].equals(item)){ed.putString("b"+i,o.optString("brand",brands[i]));ed.putString("p"+i,o.optString("price",currentPrices[i]));ed.putString("s"+i,o.optString("shop",defaultShop[i]));ed.putString("sort"+i,o.optString("variety",""));ed.putString("origin"+i,o.optString("origin",""));ed.putString("region"+i,o.optString("region",""));ed.putString("vintage"+i,o.optString("vintage",""));ed.putString("product"+i,o.optString("product",""));ed.putString("normal"+i,o.optString("regularPrice",o.optString("normalPrice","")));ed.putString("discount"+i,o.optString("discount",""));ed.putString("pack"+i,o.optString("package",""));ed.putString("total"+i,o.optString("totalQuantity",""));ed.putString("unit"+i,o.optString("unitPrice",""));ed.putString("valid"+i,o.optString("validUntil",root.optString("validFor","")));ed.putString("action"+i,o.optString("action",""));}}
      String updated=root.optString("updated",today());ed.putString("remoteDay",today()).putString("remoteUpdated",updated).apply();runOnUiThread(()->{status.setText("Aktualisiert: "+updated);render();Toast.makeText(this,"Preise aktualisiert",Toast.LENGTH_SHORT).show();});
    }catch(Exception e){runOnUiThread(()->{status.setText("Aktualisierung fehlgeschlagen – gespeicherte Preise bleiben erhalten");if(manual)Toast.makeText(this,"Keine Verbindung zur Angebotsdatei",Toast.LENGTH_LONG).show();});}}).start();}

  private int val(EditText e,int d){try{return Integer.parseInt(e.getText().toString());}catch(Exception x){return d;}}
  private void save(){prefs.edit().putString("guests",guests.getText().toString()).putString("hosts",hosts.getText().toString()).putString("days",days.getText().toString()).putString("price",price.getText().toString()).apply();}
  private String detail(int i){StringBuilder d=new StringBuilder();String product=prefs.getString("product"+i,"");String origin=prefs.getString("origin"+i,"");String region=prefs.getString("region"+i,"");String variety=prefs.getString("sort"+i,"");String vintage=prefs.getString("vintage"+i,"");String action=prefs.getString("action"+i,"");String normal=prefs.getString("normal"+i,"");String discount=prefs.getString("discount"+i,"");String pack=prefs.getString("pack"+i,"");String total=prefs.getString("total"+i,"");String unit=prefs.getString("unit"+i,"");String valid=prefs.getString("valid"+i,"");String p=prefs.getString("p"+i,currentPrices[i]);if(!product.isEmpty())d.append(product).append("\n");if(!origin.isEmpty())d.append("Herkunft: ").append(origin).append("\n");if(!region.isEmpty())d.append("Region: ").append(region).append("\n");if(!variety.isEmpty())d.append("Sorte: ").append(variety).append("\n");if(!vintage.isEmpty())d.append("Jahrgang: ").append(vintage).append("\n");if(!action.isEmpty())d.append("Aktion: ").append(action).append("\n");if(!normal.isEmpty())d.append("Normalpreis: ").append(normal).append(" € · ");if(!discount.isEmpty())d.append("Rabatt: ").append(discount).append("\n");d.append("Preis: ").append(p.equals("0")||p.equals("0.00")?"nicht verifiziert":p+" €").append("\n");if(!pack.isEmpty())d.append("Gebinde: ").append(pack).append(" · ");if(!total.isEmpty())d.append("Menge: ").append(total).append("\n");if(!unit.isEmpty())d.append("Grundpreis: ").append(unit).append("\n");if(!valid.isEmpty())d.append("Gültig bis: ").append(valid);return d.toString().trim();}

  private void render(){list.removeAllViews();int persons=val(guests,2)+val(hosts,2),d=val(days,2);double factor=(persons*d)/8.0,total=0;
    for(String shop:shops){LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);group.setPadding(dp(10),dp(10),dp(10),dp(10));group.setBackground(bg(Color.WHITE,18));group.setLayoutParams(mp(10));TextView h=tv(shop,20,true);h.setTextColor(Color.WHITE);h.setBackground(bg(GREEN,12));group.addView(h);int count=0;
      for(int i=0;i<items.length;i++)if(prefs.getString("s"+i,defaultShop[i]).equals(shop)){count++;LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setBackground(bg(Color.rgb(250,248,240),14));card.setLayoutParams(mp(8));CheckBox c=new CheckBox(this);c.setText(items[i]);c.setTextSize(17);c.setTextColor(TEXT);c.setTypeface(Typeface.DEFAULT_BOLD);card.addView(c);TextView brand=tv(prefs.getString("b"+i,brands[i]),14,true);brand.setTextColor(GREEN);brand.setPadding(dp(12),0,dp(12),dp(4));card.addView(brand);String details=detail(i);if(!details.isEmpty()){TextView info=tv(details,13,false);info.setBackground(bg(Color.rgb(255,249,226),10));card.addView(info);}LinearLayout ed=new LinearLayout(this);ed.setOrientation(LinearLayout.HORIZONTAL);ed.setGravity(Gravity.CENTER);ed.setPadding(0,dp(6),0,0);EditText q=number("q"+i,String.valueOf(Math.max(1,(int)Math.ceil(qty[i]*factor))));EditText br=new EditText(this);br.setText(prefs.getString("b"+i,brands[i]));br.setTextSize(14);br.setTextColor(TEXT);br.setBackground(bg(Color.WHITE,10));br.setPadding(dp(8),dp(10),dp(8),dp(10));EditText pr=number("p"+i,currentPrices[i]);EditText[] es={q,br,pr};for(EditText e:es){LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,-2,1);ep.setMargins(dp(3),0,dp(3),0);e.setLayoutParams(ep);ed.addView(e);}card.addView(ed);TextView caps=label("Menge        Marke / Hersteller        Preis €");caps.setGravity(Gravity.CENTER);card.addView(caps);group.addView(card);try{double pv=Double.parseDouble(pr.getText().toString().replace(',','.'));if(pv>0)total+=pv;}catch(Exception ignored){}int idx=i;q.setOnFocusChangeListener((v,f)->{if(!f)prefs.edit().putString("q"+idx,q.getText().toString()).apply();});br.setOnFocusChangeListener((v,f)->{if(!f)prefs.edit().putString("b"+idx,br.getText().toString()).apply();});pr.setOnFocusChangeListener((v,f)->{if(!f)prefs.edit().putString("p"+idx,pr.getText().toString()).apply();});}
      if(count>0)list.addView(group);
    }
    double revenue=val(guests,2)*val(days,2)*val(price,10);sum.setText(String.format(Locale.GERMANY,"Frühstücksumsatz  %.2f €\nBekannte Kosten  %.2f €   ·   Differenz  %.2f €",revenue,total,revenue-total));
  }

  private String esc(String s){if(s==null)return "";return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
  private void printList(){save();StringBuilder html=new StringBuilder();html.append("<html><head><meta charset='utf-8'><style>body{font-family:sans-serif;color:#213026;margin:28px}h1{color:#2c5b3d;margin-bottom:4px}.meta{color:#607065;margin-bottom:22px}.shop{margin-top:22px;background:#2c5b3d;color:white;padding:8px 12px;border-radius:6px;font-size:20px}.item{border-bottom:1px solid #ddd;padding:10px 4px}.name{font-size:16px;font-weight:bold}.brand{color:#2c5b3d}.details{font-size:12px;color:#555;white-space:pre-line}.price{float:right;font-weight:bold}.box{display:inline-block;width:16px;height:16px;border:1px solid #333;margin-right:8px;vertical-align:middle}.summary{margin-top:26px;padding:12px;background:#eef4ef;border-radius:8px;font-weight:bold}@page{size:A4;margin:14mm}</style></head><body>");
    html.append("<h1>Einkaufsliste – Zuhause am Bach</h1><div class='meta'>").append(new SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.GERMANY).format(new Date())).append(" · ").append(esc(prefs.getString("remoteUpdated","Preise nicht aktualisiert"))).append("</div>");double total=0;
    for(String shop:shops){StringBuilder block=new StringBuilder();for(int i=0;i<items.length;i++)if(prefs.getString("s"+i,defaultShop[i]).equals(shop)){String p=prefs.getString("p"+i,currentPrices[i]);String q=prefs.getString("q"+i,String.valueOf(qty[i]));block.append("<div class='item'><span class='box'></span><span class='name'>").append(esc(items[i])).append("</span><span class='price'>").append(esc(p.equals("0")||p.equals("0.00")?"Preis offen":p+" €")).append("</span><br><span class='brand'>").append(esc(prefs.getString("b"+i,brands[i]))).append("</span> · Menge: ").append(esc(q));String det=detail(i);if(!det.isEmpty())block.append("<div class='details'>").append(esc(det).replace("\n","<br>")) .append("</div>");block.append("</div>");try{double pv=Double.parseDouble(p.replace(',','.'));if(pv>0)total+=pv;}catch(Exception ignored){}}
      if(block.length()>0)html.append("<div class='shop'>").append(shop).append("</div>").append(block);
    }
    double revenue=val(guests,2)*val(days,2)*val(price,10);html.append("<div class='summary'>Bekannte Kosten: ").append(String.format(Locale.GERMANY,"%.2f €",total)).append(" · Frühstücksumsatz: ").append(String.format(Locale.GERMANY,"%.2f €",revenue)).append("</div></body></html>");
    WebView web=new WebView(this);web.loadDataWithBaseURL(null,html.toString(),"text/html","UTF-8",null);web.setWebViewClient(new android.webkit.WebViewClient(){public void onPageFinished(WebView view,String url){PrintManager pm=(PrintManager)getSystemService(Context.PRINT_SERVICE);pm.print("Einkaufsliste Zuhause am Bach",view.createPrintDocumentAdapter("Einkaufsliste Zuhause am Bach"),new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());}});
  }
}
