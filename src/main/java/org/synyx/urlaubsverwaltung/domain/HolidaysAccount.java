/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.synyx.urlaubsverwaltung.domain;

import org.springframework.data.jpa.domain.AbstractPersistable;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;


/**
 * @author  Johannes Reuter
 * @author  Aljona Murygina
 */
@Entity
public class HolidaysAccount extends AbstractPersistable<Integer> {

    private static final long serialVersionUID = 341278947843289L;

    // One person may have multiple accounts (depending on years)
    @ManyToOne
    private Person person;

    // Year of account
    private int year;

    // Residual number of leave days that person has
    private BigDecimal vacationDays;

    // Residual number of remaining days of vacation that person has
    private BigDecimal remainingVacationDays;

    // Number of days that person has used this year
    private BigDecimal usedVacationDays;

    // Number of days of special leave used this year
    private BigDecimal sonderUrlaub;

    // Number of days of unpaid leave used this year
    private BigDecimal unbezahlterUrlaub;

    public Person getPerson() {

        return person;
    }


    public void setPerson(Person person) {

        this.person = person;
    }


    public BigDecimal getRemainingVacationDays() {

        return remainingVacationDays;
    }


    public void setRemainingVacationDays(BigDecimal remainingVacationDays) {

        this.remainingVacationDays = remainingVacationDays;
    }


    public BigDecimal getSonderUrlaub() {

        return sonderUrlaub;
    }


    public void setSonderUrlaub(BigDecimal sonderUrlaub) {

        this.sonderUrlaub = sonderUrlaub;
    }


    public BigDecimal getUnbezahlterUrlaub() {

        return unbezahlterUrlaub;
    }


    public void setUnbezahlterUrlaub(BigDecimal unbezahlterUrlaub) {

        this.unbezahlterUrlaub = unbezahlterUrlaub;
    }


    public BigDecimal getUsedVacationDays() {

        return usedVacationDays;
    }


    public void setUsedVacationDays(BigDecimal usedVacationDays) {

        this.usedVacationDays = usedVacationDays;
    }


    public BigDecimal getVacationDays() {

        return vacationDays;
    }


    public void setVacationDays(BigDecimal vacationDays) {

        this.vacationDays = vacationDays;
    }


    public int getYear() {

        return year;
    }


    public void setYear(int year) {

        this.year = year;
    }
}
