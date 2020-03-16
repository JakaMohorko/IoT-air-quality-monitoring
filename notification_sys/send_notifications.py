#!/bin/python3

import smtplib
import sqlite3
import os
import ssl
from google.cloud import bigquery

def alt_send_email(email_address, aqiCat):

    smtp_servers = ["smtp.gmail.com", "smtp.live.com",]
    email_send_from = ['***REMOVED***','***REMOVED***', "gassie_edinburgh@outlook.com"]
    
    # from and to
    fromaddr = email_send_from[2]
    toaddrs = email_address
    
    # credentials
    username = email_send_from[2]
    ***REMOVED***

    context = ssl.create_default_context()
    
    msg = "\r\n".join([
        "From: " + fromaddr,
        "To: " + email_address,
        "Subject: Air Quality alert",
        "",
        "",
        "Hello " + toaddrs,
        "",
        "The air quality in has been recorded as " + aqiCat 
    ])

    server = smtplib.SMTP(smtp_servers[1], 587)
    server.ehlo()
    server.starttls()
    server.ehlo()
    server.login(username, password)
    server.sendmail(fromaddr, toaddrs, msg)
    server.quit()

    
    # with smtplib.SMTP_SSL(smtp_servers[1], 465, context=context) as server:
    #    server.login(username, password)
    #    server.sendmail(fromaddr, toaddrs, msg)


def send_email(email_address, aqiCat):
    msg = "\r\n".join([
        "From: ***REMOVED***",
        "To: " + email_address,
        "Subject: Air Quality alert",
        "",
        "Hello " + email_address,
        ""
        "The air quality in has been recorded as " + aqiCat 
    ])

    # from and to
    fromaddr = '***REMOVED***'
    toaddrs = email_address
    
    # credentials
    username = '***REMOVED***'
    password = input("Enter password: ")

    server = smtplib.SMTP('smtp.gmail.com:587')
    server.ehlo()
    server.starttls()
    server.ehlo()
    server.login(username, password)
    server.sendmail(fromaddr, toaddrs, msg)
    server.quit()

def test_extract_schema(client): 
    project = 'gassie'
    dataset_id = 'AQIreadings'
    table_id = 'readings'
    dataset_ref = client.dataset(dataset_id, project=project)
    table_ref = dataset_ref.table(table_id)
    table = client.get_table(table_ref)  # API Request

    field_names = []
    for field in table.schema:
        field_names.append(field.name)

    print(field_names)

def get__aqi_value():
    # access big table
    client = bigquery.Client()

    query = """
         SELECT * FROM AQIreadings.readings as valReadings 
            INNER JOIN (
                SELECT MAX(TIME) as MAX_TIME 
                FROM AQIreadings.readings) 
                ON valReadings.TIME = MAX_TIME;
    """

    query_job = client.query(query)

    query_res = list(query_job)

    # get_schema(client)

    if len(query_res) < 1:
        print("ERROR: query messed up")
        return None

    # get latest aqi value or its rating
    return query_res[0]
    
    
def get_alert_contacts():
    # database location
    db_loc = "/public/homepages/s1541472/web/alert_contacts.db"

    db_connection = sqlite3.connect(db_loc)
    cur = db_connection.cursor()

    cur.execute("select Email, AqiLevel from ADDRESSES;")
    # only get email address from the database row
    emails_adr = [ e for e in cur.fetchall()]
    
    return emails_adr

def print_query_row(query_row):
    query_feild_names = ['AQI', 'location_tag', 'AQIco', 'AQIno2', 'AQIdust', 'TIME', 'longitude', 'latitude', 'AQIcategory']
    for field in query_feild_names:
        print("{}: {}".format(field, query_row[field]))

def notify_contacts():

    AQIcatVals = {"Good": 0,
                    "Moderate": 1, 
                    "Unhealthy for Sensitive Groups": 2, 
                    "Unhealthy": 3,
                    "Very Unhealthy": 4,
                    "Hazardous": 5
                }

    # get_aqi_value
    newest_AQI = get__aqi_value()
    currentAQIcat = newest_AQI['AQIcategory']

    print("=== Most recent AQI===")
    print_query_row(newest_AQI)
    
    # get emails
    user_contacts = get_alert_contacts();
    
    # for each email address send mail if level of pollution met
    print("=== EMAILS ===")
    for user_pref in user_contacts:
        
        print("Email: {},  AQI category: {}".format(user_pref[0], user_pref[1]))
        
        # check if current AQI category is greater or equal to users alert preference
        if AQIcatVals[currentAQIcat]  >= AQIcatVals[user_pref[1]]:
            alt_send_email(user_pref[0], currentAQIcat)

            
def main():
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = "/afs/inf.ed.ac.uk/user/s15/s1541472/Documents/classes/iotssc/notification_sys/gassie-53e7e2ca6f44.json"
    notify_contacts()


if __name__ == '__main__':
    main()
