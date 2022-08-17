package com.legacy.server.external;

import java.util.ArrayList;


public class CerterDef {
    /**
     * Certs this stall can deal with
     */
	private ArrayList<CertDef> certs;
    /**
     * Type of stall
     */
    private String type;

    public CerterDef(String type) {
		this.type = type;
		this.certs = new ArrayList<CertDef>();
	}
    
    public ArrayList<CertDef> getCerts() {
		return certs;
	}

    public int getCertID(int index) {
		int ret = -1;
		if(certs.get(index) != null) {
			ret = certs.get(index).getCertID();
		}
		return ret;
	}
	
	public String[] getCertNames() {
		String[] names = new String[certs.size()];
		int counter = 0;
		for(CertDef cert : certs) {
			names[counter] = cert.getName();
			counter++;
		}
		return names;
	}

    /*public String[] getCertNames() {
        String[] names = new String[certs.length];
        for (int i = 0; i < certs.length; i++) {
            names[i] = certs[i].getName();
        }
        return names;
    }*/

	public int getItemID(int index) {
		int ret = -1;
		if(certs.get(index) != null) {
			ret = certs.get(index).getItemID();
		}
		return ret;
	}

    public String getType() {
        return type;
    }
}