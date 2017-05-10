package models;

/**
 * Created by vladi on 11-Apr-17.
 */
public class RestSection {

    public String sectionName;
    public String sectionColor;
    public String posX;
    public String posY;
    public RestSection(String sectionColor, String posX, String posY) {

        this.sectionColor = sectionColor;
        this.posX = posX;
        this.posY = posY;
    }
    public RestSection(String sectionName, String sectionColor) {
        this.sectionName = sectionName;
        this.sectionColor = sectionColor;
    }
    public RestSection() { }
}
