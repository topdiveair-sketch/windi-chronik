<?php
require __DIR__.'/config.php';
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, X-Admin-Token');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
if($_SERVER['REQUEST_METHOD']==='OPTIONS'){http_response_code(204);exit;}
if(!is_dir(dirname(DATA_FILE))) mkdir(dirname(DATA_FILE),0755,true);
if(!file_exists(DATA_FILE)) file_put_contents(DATA_FILE,'[]',LOCK_EX);
function out($x,$code=200){http_response_code($code);echo json_encode($x,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES);exit;}
function readItems(){return json_decode(file_get_contents(DATA_FILE),true) ?: [];}
function writeItems($x){$tmp=DATA_FILE.'.tmp';file_put_contents($tmp,json_encode(array_values($x),JSON_PRETTY_PRINT|JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES),LOCK_EX);rename($tmp,DATA_FILE);}
function admin(){return hash_equals(ADMIN_TOKEN,$_SERVER['HTTP_X_ADMIN_TOKEN']??'');}
function body(){return json_decode(file_get_contents('php://input'),true) ?: [];}
$a=$_GET['action']??'ping';
if($a==='ping') out(['ok'=>true,'service'=>'Windi-Chronik']);
if($a==='list'){ $items=readItems(); if(!admin()) $items=array_values(array_filter($items,fn($x)=>($x['status']??'')==='published')); out(['ok'=>true,'items'=>$items]); }
if($a==='submit' && $_SERVER['REQUEST_METHOD']==='POST'){ $x=body(); foreach(['id','date','type','route','story'] as $k) if(empty($x[$k])) out(['ok'=>false,'error'=>'Pflichtfeld fehlt: '.$k],400); $x['status']='pending';$x['createdAt']=date(DATE_ATOM);$items=readItems();$items[]=$x;writeItems($items);out(['ok'=>true,'id'=>$x['id']]); }
if(!admin()) out(['ok'=>false,'error'=>'Redaktionscode ist falsch.'],401);
if($a==='update'){ $x=body();$items=readItems();$found=false;foreach($items as &$v)if(($v['id']??'')===($x['id']??'')){$v=$x;$found=true;break;}if(!$found)out(['ok'=>false,'error'=>'Beitrag nicht gefunden.'],404);writeItems($items);out(['ok'=>true]); }
if($a==='delete'){ $id=body()['id']??'';$items=array_values(array_filter(readItems(),fn($x)=>($x['id']??'')!==$id));writeItems($items);out(['ok'=>true]); }
if($a==='import'){ $items=body()['items']??null;if(!is_array($items))out(['ok'=>false,'error'=>'Ungültige Importdatei.'],400);writeItems($items);out(['ok'=>true]); }
if($a==='clear'){writeItems([]);out(['ok'=>true]);}
out(['ok'=>false,'error'=>'Unbekannte Aktion.'],404);
