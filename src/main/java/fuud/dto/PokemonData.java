package fuud.dto;

import java.util.Random;

public class PokemonData {
    private int cp;
    private int hp;
    private float level;
    private int individualAttack;
    private int individualDefence;
    private int individualStamina;
    private String pokemonId;
    private boolean favorite;
    private int attack;
    private int defence;
    private int stamina;
    private String maxCpForYourLevel;
    private String maxCpForMaxLevel;
    private int remindingDustForYourLevel;
    private int remindingDustForMaxLevel;
    private int remindingCandiesForYourLevel;
    private int remindingCandiesForMaxLevel;
    private long creationTimeMs;
    private boolean firstAttackMatch;
    private boolean secondAttackMatch;

    public PokemonData() {
    }

    // for testing
    public PokemonData(int cp, int hp, float level, int individualAttack, int individualDefence, int individualStamina) {
        this.cp = cp;
        this.hp = hp;
        this.level = level;
        this.individualAttack = individualAttack;
        this.individualDefence = individualDefence;
        this.individualStamina = individualStamina;
        this.pokemonId = "" +new Random().nextLong();
        this.creationTimeMs = System.currentTimeMillis();
    }

    public void setCp(int cp) {
        this.cp = cp;
    }

    public int getCp() {
        return cp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getHp() {
        return hp;
    }

    public void setLevel(float level) {
        this.level = level;
    }

    public float getLevel() {
        return level;
    }

    public void setIndividualAttack(int individualAttack) {
        this.individualAttack = individualAttack;
    }

    public int getIndividualAttack() {
        return individualAttack;
    }

    public void setIndividualDefence(int individualDefence) {
        this.individualDefence = individualDefence;
    }

    public int getIndividualDefence() {
        return individualDefence;
    }

    public void setIndividualStamina(int individualStamina) {
        this.individualStamina = individualStamina;
    }

    public int getIndividualStamina() {
        return individualStamina;
    }

    public void setPokemonId(String pokemonId) {
        this.pokemonId = pokemonId;
    }

    public String getPokemonId() {
        return pokemonId;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getAttack() {
        return attack;
    }

    public void setDefence(int defence) {
        this.defence = defence;
    }

    public int getDefence() {
        return defence;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public int getStamina() {
        return stamina;
    }

    public String getMaxCpForYourLevel() {
        return maxCpForYourLevel;
    }

    public void setMaxCpForYourLevel(String maxCpForYourLevel) {
        this.maxCpForYourLevel = maxCpForYourLevel;
    }

    public String getMaxCpForMaxLevel() {
        return maxCpForMaxLevel;
    }

    public void setMaxCpForMaxLevel(String maxCpForMaxLevel) {
        this.maxCpForMaxLevel = maxCpForMaxLevel;
    }

    public void setRemindingDustForYourLevel(int remindingDustForYourLevel) {
        this.remindingDustForYourLevel = remindingDustForYourLevel;
    }

    public int getRemindingDustForYourLevel() {
        return remindingDustForYourLevel;
    }

    public void setRemindingDustForMaxLevel(int remindingDustForMaxLevel) {
        this.remindingDustForMaxLevel = remindingDustForMaxLevel;
    }

    public int getRemindingDustForMaxLevel() {
        return remindingDustForMaxLevel;
    }

    public void setRemindingCandiesForYourLevel(int remindingCandiesForYourLevel) {
        this.remindingCandiesForYourLevel = remindingCandiesForYourLevel;
    }

    public int getRemindingCandiesForYourLevel() {
        return remindingCandiesForYourLevel;
    }

    public void setRemindingCandiesForMaxLevel(int remindingCandiesForMaxLevel) {
        this.remindingCandiesForMaxLevel = remindingCandiesForMaxLevel;
    }

    public int getRemindingCandiesForMaxLevel() {
        return remindingCandiesForMaxLevel;
    }

    public void setCreationTimeMs(long creationTimeMs) {
        this.creationTimeMs = creationTimeMs;
    }

    public long getCreationTimeMs() {
        return creationTimeMs;
    }

    public boolean isFirstAttackMatch() {
        return firstAttackMatch;
    }

    public void setFirstAttackMatch(boolean firstAttackMatch) {
        this.firstAttackMatch = firstAttackMatch;
    }

    public boolean isSecondAttackMatch() {
        return secondAttackMatch;
    }

    public void setSecondAttackMatch(boolean secondAttackMatch) {
        this.secondAttackMatch = secondAttackMatch;
    }
}
