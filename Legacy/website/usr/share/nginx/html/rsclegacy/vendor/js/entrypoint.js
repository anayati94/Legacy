var webcalcs={};webcalcs.combat=new combat,webcalcs.maxHit=new maxHit,webcalcs.firemaking=new firemaking,webcalcs.expTable=new expTable,pageLoaded=function(){try{webcalcs.combat.init("combatLvlResult",["attack","defense","strength","hits","ranged","prayer","magic"]),webcalcs.maxHit.init(document.MaxHit.Result,document.MaxHit.NR)}catch(a){}};